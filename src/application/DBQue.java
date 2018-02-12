package application;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.omg.PortableServer.REQUEST_PROCESSING_POLICY_ID;

public class DBQue {
	Connection con = null;
	PreparedStatement pstmt;
	ResultSet rs;
	public List<String> getDB() {
		List<String> list =  new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader("C:/Uni_Cool/KCM_IP.DAT"));
			String ip = br.readLine();
			String license = br.readLine();
			list.add(ip);
			list.add(license);
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	public Connection dbConn() {
			List<String> list = getDB();
			String DBurl = "jdbc:sqlserver://"+list.get(0)+":1433;databaseName="+list.get(1);
			String user = "sa";
			String pwd = "unicool";
			try {
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				con = DriverManager.getConnection(DBurl, user, pwd);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return con;
	}
	public ResultSet getRS(String sql) throws SQLException {
		con = dbConn();
		pstmt = con.prepareStatement(sql);
		rs = pstmt.executeQuery();
		return rs;
	}
	
	public void insertDB(String sql) throws SQLException {
		con = dbConn();
		pstmt = con.prepareStatement(sql);
		pstmt.executeUpdate();
		closeDB();
	}
	
	public void deleteDB(String sql) throws SQLException{
		con = dbConn();
		pstmt = con.prepareStatement(sql);
		pstmt.executeUpdate();
		closeDB();
	}
	
	public void closeDB() {
		if(rs!=null) {try {rs.close();}catch(SQLException e){}}
		if(pstmt!=null) {try {pstmt.close();}catch(SQLException e){}}
		if(con!=null) {try {con.close();}catch(SQLException e){}}
	}
	
	public ResultSet getCount(String sql) throws SQLException {
		con = dbConn();
		pstmt = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		return rs;
	}
}
