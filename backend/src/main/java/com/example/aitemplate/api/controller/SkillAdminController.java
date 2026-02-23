package com.example.aitemplate.api.controller;

import com.example.aitemplate.api.dto.SkillImportShRequest;
import com.example.aitemplate.api.dto.SkillImportShResponse;
import com.example.aitemplate.api.dto.SkillImportSourceRequest;
import com.example.aitemplate.api.dto.SkillInfo;
import com.example.aitemplate.api.dto.SkillUpsertRequest;
import com.example.aitemplate.app.SkillRegistry;
import com.example.aitemplate.app.SkillSourceImportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/skills")
@Validated
public class SkillAdminController {

    private final SkillRegistry skillRegistry;
    private final SkillSourceImportService skillSourceImportService;

    public SkillAdminController(SkillRegistry skillRegistry, SkillSourceImportService skillSourceImportService) {
        this.skillRegistry = skillRegistry;
        this.skillSourceImportService = skillSourceImportService;
    }

    @GetMapping
    public List<SkillInfo> list() {
        return skillRegistry.listEntries().stream()
                .map(entry -> new SkillInfo(
                        entry.provider().skillName(),
                        entry.provider().version(),
                        entry.source(),
                        entry.editable()))
                .toList();
    }

    @PostMapping
    public SkillInfo upsert(@Valid @RequestBody SkillUpsertRequest request) {
        var provider = skillRegistry.upsertDynamic(request.skillName(), request.version(), request.content());
        return new SkillInfo(provider.skillName(), provider.version(), "dynamic", true);
    }

    @DeleteMapping
    public void delete(
            @RequestParam String skillName,
            @RequestParam(required = false) String version) {
        if (!skillRegistry.deleteDynamic(skillName, version)) {
            throw new IllegalArgumentException("Dynamic skill not found: " + skillName + (version == null ? "" : "@" + version));
        }
    }

    @PostMapping("/import-sh")
    public SkillImportShResponse importSkillsSh(@Valid @RequestBody SkillImportShRequest request) {
        var result = skillRegistry.importFromSkillsSh(request.script());
        return new SkillImportShResponse(result.imported(), result.errors());
    }

    @PostMapping("/import-source")
    public SkillImportShResponse importSource(@Valid @RequestBody SkillImportSourceRequest request) {
        var result = skillSourceImportService.importFromSource(request.source());
        return new SkillImportShResponse(result.imported(), result.errors());
    }
}
