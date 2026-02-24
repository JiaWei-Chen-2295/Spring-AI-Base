package com.example.aitemplate.infra.db.typehandler;

import com.example.aitemplate.core.model.CapabilitySet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MappedTypes(CapabilitySet.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class CapabilitySetTypeHandler extends BaseTypeHandler<CapabilitySet> {

    private static final Logger log = LoggerFactory.getLogger(CapabilitySetTypeHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
            CapabilitySet parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize CapabilitySet", e);
            ps.setNull(i, java.sql.Types.VARCHAR);
        }
    }

    @Override
    public CapabilitySet getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public CapabilitySet getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public CapabilitySet getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private CapabilitySet parse(String json) {
        if (json == null || json.isBlank()) return CapabilitySet.chatOnly();
        try {
            return MAPPER.readValue(json, CapabilitySet.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize CapabilitySet: {}", json, e);
            return CapabilitySet.chatOnly();
        }
    }
}
