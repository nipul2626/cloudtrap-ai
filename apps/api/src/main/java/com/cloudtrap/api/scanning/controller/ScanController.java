package com.cloudtrap.api.scanning.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class ScanController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ScanController(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc=jdbc; this.mapper=mapper; }

    public record CreateScanRequest(@NotNull UUID workspaceId,
                                    @NotBlank @Size(max=120)
                                    @Pattern(regexp="[a-zA-Z0-9][a-zA-Z0-9._-]*") String datasetId) {}

    @PostMapping("/scans")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String,Object> create(@Valid @RequestBody CreateScanRequest request,
                                     @RequestHeader(value="Idempotency-Key", required=false) String key,
                                     Authentication authentication) throws Exception {
        UUID userId = currentUserId(authentication);
        requireMembership(request.workspaceId(), userId);
        if (!"default-demo".equals(request.datasetId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported dataset");
        }
        if (key != null && (key.isBlank() || key.length()>128 || !key.matches("[A-Za-z0-9._:-]+"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key");
        }
        String fingerprint = sha256(request.workspaceId()+"|"+request.datasetId());
        if (key != null) {
            List<Map<String,Object>> prior = jdbc.queryForList(
                "SELECT id,request_fingerprint FROM scans WHERE workspace_id=? AND idempotency_key=?",
                request.workspaceId(), key);
            if (!prior.isEmpty()) {
                if (!fingerprint.equals(((String)prior.getFirst().get("request_fingerprint")).trim())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was used for a different request");
                }
                return response((UUID)prior.getFirst().get("id"), request.workspaceId());
            }
        }
        UUID id = UUID.randomUUID();
        String ruleVersion = jdbc.queryForList("SELECT rule_id,version FROM security_rules WHERE enabled=true ORDER BY rule_id,version")
            .stream().map(row -> row.get("rule_id")+"@"+row.get("version")).reduce((a,b)->a+","+b).orElse("empty");
        try {
            jdbc.update("""
              INSERT INTO scans(id,workspace_id,requested_by,source,dataset_id,status,rule_set_version,
                risk_formula_version,idempotency_key,request_fingerprint,max_attempts)
              VALUES(?,?,?,'DEMO',?,'QUEUED',?,'cloudtrap-risk-v1',?,?,3)
              """, id, request.workspaceId(), userId, request.datasetId(), ruleVersion, key, fingerprint);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            if (key == null) throw ex;
            List<Map<String,Object>> prior = jdbc.queryForList(
                "SELECT id,request_fingerprint FROM scans WHERE workspace_id=? AND idempotency_key=?",
                request.workspaceId(), key);
            if (prior.isEmpty() || !fingerprint.equals(((String)prior.getFirst().get("request_fingerprint")).trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key conflict");
            }
            id=(UUID)prior.getFirst().get("id");
        }
        return response(id, request.workspaceId());
    }

    @GetMapping("/scans/{scanId}")
    public Map<String,Object> get(@PathVariable UUID scanId, @RequestParam UUID workspaceId, Authentication authentication) {
        UUID userId=currentUserId(authentication); requireMembership(workspaceId,userId);
        return response(scanId,workspaceId);
    }

    @GetMapping("/scans/{scanId}/findings")
    public Map<String,Object> findings(@PathVariable UUID scanId, @RequestParam UUID workspaceId,
                                      @RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="50") int size, Authentication authentication) {
        UUID userId=currentUserId(authentication); requireMembership(workspaceId,userId);
        if(page<0 || size<1 || size>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination");
        Map<String,Object> scan=response(scanId,workspaceId);
        int offset=Math.multiplyExact(page,size);
        List<Map<String,Object>> rows=jdbc.queryForList("""
          SELECT f.id,f.asset_id,f.rule_id,f.rule_version,f.title,f.description,f.category,f.severity,f.status,
                 f.evidence,f.first_detected_at,f.last_detected_at,
                 (SELECT ra.score FROM risk_assessments ra WHERE ra.finding_id=f.id ORDER BY ra.calculated_at DESC,ra.id DESC LIMIT 1) AS risk_score
          FROM findings f WHERE f.workspace_id=? AND f.scan_id=?
          ORDER BY CASE f.severity WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END,
                   f.created_at DESC,f.id
          LIMIT ? OFFSET ?
          """,workspaceId,scanId,size,offset);
        Integer total=jdbc.queryForObject("SELECT COUNT(*) FROM findings WHERE workspace_id=? AND scan_id=?",Integer.class,workspaceId,scanId);
        return Map.of("scan",scan,"items",rows,"page",page,"size",size,"total",total==null?0:total);
    }


    public record FindingStatusRequest(@NotBlank @Pattern(regexp="ACKNOWLEDGED|RESOLVED|ACCEPTED_RISK|OPEN") String status) {}

    @PatchMapping("/findings/{findingId}/status")
    public Map<String,Object> updateFindingStatus(@PathVariable UUID findingId,
            @RequestParam UUID workspaceId, @Valid @RequestBody FindingStatusRequest request,
            Authentication authentication) {
        UUID userId=currentUserId(authentication); requireMembership(workspaceId,userId);
        int updated=jdbc.update("""
          UPDATE findings SET status=?, resolved_at=CASE WHEN ?='RESOLVED' THEN CURRENT_TIMESTAMP ELSE NULL END,
            updated_at=CURRENT_TIMESTAMP WHERE id=? AND workspace_id=?
          """,request.status(),request.status(),findingId,workspaceId);
        if(updated!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Finding not found");
        return jdbc.queryForList("""
          SELECT id,workspace_id,scan_id,asset_id,rule_id,rule_version,title,severity,status,resolved_at,updated_at
          FROM findings WHERE id=? AND workspace_id=?
          """,findingId,workspaceId).getFirst();
    }

    @GetMapping("/assets")
    public Map<String,Object> listAssets(@RequestParam UUID workspaceId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size,
            Authentication authentication) {
        UUID userId=currentUserId(authentication); requireMembership(workspaceId,userId);
        if(page<0 || size<1 || size>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination");
        int offset=Math.multiplyExact(page,size);
        List<Map<String,Object>> items=jdbc.queryForList("""
          SELECT id,provider,external_id,asset_type,name,region,exposure,criticality,first_seen_at,last_seen_at
          FROM assets WHERE workspace_id=? ORDER BY last_seen_at DESC,id LIMIT ? OFFSET ?
          """,workspaceId,size,offset);
        Integer total=jdbc.queryForObject("SELECT COUNT(*) FROM assets WHERE workspace_id=?",Integer.class,workspaceId);
        return Map.of("items",items,"page",page,"size",size,"total",total==null?0:total);
    }

    @GetMapping("/assets/{assetId}")
    public Map<String,Object> getAsset(@PathVariable UUID assetId,@RequestParam UUID workspaceId,
            Authentication authentication) {
        UUID userId=currentUserId(authentication); requireMembership(workspaceId,userId);
        List<Map<String,Object>> rows=jdbc.queryForList("""
          SELECT a.id,a.provider,a.external_id,a.asset_type,a.name,a.region,a.exposure,a.criticality,
                 a.normalized_attributes,a.first_seen_at,a.last_seen_at,
                 (SELECT COUNT(*) FROM findings f WHERE f.workspace_id=a.workspace_id AND f.asset_id=a.id AND f.status='OPEN') AS open_findings
          FROM assets a WHERE a.id=? AND a.workspace_id=?
          """,assetId,workspaceId);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Asset not found");
        return rows.getFirst();
    }

    private Map<String,Object> response(UUID id, UUID workspaceId) {
        List<Map<String,Object>> rows=jdbc.queryForList("""
          SELECT id,workspace_id,dataset_id,source,status,rule_set_version,risk_formula_version,queued_at,
                 started_at,completed_at,assets_discovered,assets_scanned,findings_created,error_code,safe_error_message,
                 attempt_count,max_attempts
          FROM scans WHERE id=? AND workspace_id=?
          """,id,workspaceId);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Scan not found");
        return rows.getFirst();
    }
    private UUID currentUserId(Authentication authentication) {
        if(authentication==null || !authentication.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        List<UUID> ids=jdbc.query("SELECT id FROM users WHERE lower(email)=lower(?)", (rs,n)->rs.getObject(1,UUID.class), authentication.getName());
        if(ids.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return ids.getFirst();
    }
    private void requireMembership(UUID workspaceId, UUID userId) {
        Boolean ok=jdbc.queryForObject("""
          SELECT EXISTS(SELECT 1 FROM workspaces w JOIN workspace_memberships m ON m.workspace_id=w.id
          WHERE w.id=? AND m.user_id=?)
          """,Boolean.class,workspaceId,userId);
        if(!Boolean.TRUE.equals(ok)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Workspace not found");
    }
    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
