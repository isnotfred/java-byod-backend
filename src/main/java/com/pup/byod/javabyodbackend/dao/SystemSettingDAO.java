package com.pup.byod.javabyodbackend.dao;

import com.pup.byod.javabyodbackend.model.SystemSetting;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SystemSettingDAO {

    private final NamedParameterJdbcTemplate jdbc;

    public SystemSettingDAO(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<SystemSetting> settingRowMapper = (rs, rowNum) -> SystemSetting.builder()
            .settingKey(rs.getString("setting_key"))
            .settingValue(rs.getString("setting_value"))
            .description(rs.getString("description"))
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public List<SystemSetting> findAll() {
        String sql = "SELECT * FROM system_settings ORDER BY setting_key";
        return jdbc.query(sql, settingRowMapper);
    }

    public Optional<SystemSetting> findByKey(String key) {
        String sql = "SELECT * FROM system_settings WHERE setting_key = :key";
        var params = new MapSqlParameterSource("key", key);
        return jdbc.query(sql, params, settingRowMapper).stream().findFirst();
    }

    public int update(String key, String value) {
        String sql = "UPDATE system_settings SET setting_value = :value WHERE setting_key = :key";
        var params = new MapSqlParameterSource()
                .addValue("value", value)
                .addValue("key", key);
        return jdbc.update(sql, params);
    }
}
