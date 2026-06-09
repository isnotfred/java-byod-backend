package com.pup.byod.javabyodbackend.controller;

import com.pup.byod.javabyodbackend.model.SystemSetting;
import com.pup.byod.javabyodbackend.service.SystemSettingService;
import com.pup.byod.javabyodbackend.util.ValidationUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
public class SystemSettingController {

    private final SystemSettingService settingsService;

    public SystemSettingController(SystemSettingService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public List<SystemSetting> getAllSettings() {
        return settingsService.getAllSettings();
    }

    @PutMapping("/{key}")
    public ResponseEntity<SystemSetting> updateSetting(
            @PathVariable String key,
            @RequestBody UpdateSettingRequest request
    ) {
        ValidationUtil.requireNonNull(request.actingUserId, "actingUserId");
        ValidationUtil.requireNonBlank(request.settingValue, "settingValue");

        SystemSetting updated = settingsService.updateSetting(
                request.actingUserId,
                key,
                request.settingValue
        );
        return ResponseEntity.ok(updated);
    }

    public static class UpdateSettingRequest {
        public Integer actingUserId;
        public String settingValue;
    }
}
