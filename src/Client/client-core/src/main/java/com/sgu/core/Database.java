package com.sgu.core;
import java.sql.*;
import java.util.*;
public final class Database {
    private final Settings s;
    public Database(Settings s) {
        this.s=s;
    }
    public Connection open()throws SQLException {
        String password=s.get("sgu.db.password","");
        if(password.isBlank())throw new SQLException("SGU_DB_PASSWORD is required in live mode");
        return DriverManager.getConnection(s.get("sgu.db.url","jdbc:mysql://127.0.0.1:3306/SGU_audio_guide?connectionTimeZone=UTC"),s.get("sgu.db.username","sgu_app"),password);
    }
    public static List<Map<String,Object>> rows(Connection c,String sql,Object...args)throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {
            bind(p,args);
            try(ResultSet r=p.executeQuery()) {
                List<Map<String,Object>> out=new ArrayList<>();
                ResultSetMetaData md=r.getMetaData();
                while(r.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=md.getColumnCount();i++) {
                        Object v=r.getObject(i);
                        if(v instanceof byte[])continue;
                        if(v instanceof java.time.temporal.TemporalAccessor||v instanceof java.util.Date)v=v.toString();
                        row.put(md.getColumnLabel(i),v);
                    }
                    out.add(row);
                }
                return out;
            }
        }
    }
    public static void bind(PreparedStatement p,Object...args)throws SQLException {
        for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);
    }
    public static int update(Connection c,String sql,Object...args)throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {
            bind(p,args);
            return p.executeUpdate();
        }
    }
    public static long insert(Connection c,String sql,Object...args)throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            bind(p,args);
            p.executeUpdate();
            try(ResultSet r=p.getGeneratedKeys()) {
                if(!r.next())throw new SQLException("Missing generated ID");
                return r.getLong(1);
            }
        }
    }
    public static Http.Error friendly(SQLException e) {
        String state=e.getSQLState();
        if(state!=null&&state.startsWith("23"))return new Http.Error(409,"Dữ liệu trùng hoặc liên kết không hợp lệ. Kiểm tra POI, ngôn ngữ và audio.");
        return new Http.Error(503,"Chưa truy cập được MySQL. Kiểm tra SGU_DB_PASSWORD và schema SGU_audio_guide.");
    }
}
