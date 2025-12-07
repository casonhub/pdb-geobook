package com.geobook;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import oracle.sql.STRUCT;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SdoGeometryType implements UserType<String> {

    @Override
    public int getSqlType() {
        return java.sql.Types.STRUCT;
    }

    @Override
    public Class<String> returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String x, String y) {
        return x == y || (x != null && x.equals(y));
    }

    @Override
    public int hashCode(String x) {
        return x.hashCode();
    }

    @Override
    public String nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        STRUCT struct = (STRUCT) rs.getObject(position);
        if (struct == null) return null;

        Connection conn = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT SDO_UTIL.TO_WKTGEOMETRY(?) FROM DUAL");
        ps.setObject(1, struct);
        ResultSet rs2 = ps.executeQuery();
        String wkt = null;
        if (rs2.next()) {
            wkt = rs2.getString(1);
        }
        rs2.close();
        ps.close();
        return wkt;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, String value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, getSqlType());
        } else {
            Connection conn = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT SDO_UTIL.FROM_WKTGEOMETRY(?) FROM DUAL");
            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            STRUCT struct = null;
            if (rs.next()) {
                struct = (STRUCT) rs.getObject(1);
            }
            rs.close();
            ps.close();
            st.setObject(index, struct);
        }
    }

    @Override
    public String deepCopy(String value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String value) {
        return value;
    }

    @Override
    public String assemble(Serializable cached, Object owner) {
        return (String) cached;
    }

    @Override
    public String replace(String detached, String managed, Object owner) {
        return detached;
    }
}