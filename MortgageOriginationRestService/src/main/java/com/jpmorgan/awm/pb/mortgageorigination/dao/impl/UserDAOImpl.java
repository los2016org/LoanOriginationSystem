package com.jpmorgan.awm.pb.mortgageorigination.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.jpmorgan.awm.pb.mortgageorigination.dao.UserDAO;
import com.jpmorgan.awm.pb.mortgageorigination.response.UserDetailsResponse;
import com.myorg.losmodel.model.LOSResponse;
import com.myorg.losmodel.model.User;
import com.myorg.losmodel.model.UserInfo;

@Service
public class UserDAOImpl implements UserDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public boolean addUser(UserInfo userInfo) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public UserDetailsResponse authenticateUser(String userId, String password) {
		UserDetailsResponse userDetailsResponse = new UserDetailsResponse();

		String sql = "select * from USERS where USER_CD = ?";
		User user = new User();
		try {
			user = jdbcTemplate.queryForObject(sql, new Object[] { userId }, new UsersRowMapper());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOSResponse messageResponse = new LOSResponse();
			messageResponse.setReturnMsg("Login Failed");
			messageResponse.setReturnType("Error");
			userDetailsResponse.setResponse(messageResponse);
			return userDetailsResponse;
		}

		if (user != null && user.getPassword().equalsIgnoreCase(password)) {
			LOSResponse messageResponse = new LOSResponse();

			messageResponse.setReturnMsg("User logged in Sucessfully");
			messageResponse.setReturnType("Success");
			userDetailsResponse.setResponse(messageResponse);
			if (user.getRoleId() == 1000) {
				user.setUserType("A");
			} else {
				user.setUserType("C");
			}
			userDetailsResponse.setUser(user);
		} else {
			LOSResponse messageResponse = new LOSResponse();
			messageResponse.setReturnMsg("Login Failed");
			messageResponse.setReturnType("Error");
			userDetailsResponse.setResponse(messageResponse);
		}
		return userDetailsResponse;
	}

	public List<User> getAllUsers() {
		String sql = "select * from USERS where USER_CD";
		List<User> userList = jdbcTemplate.query(sql, new UsersRowMapper());
		return userList;
	}

	private class UsersRowMapper implements RowMapper<User> {

		public User mapRow(ResultSet rs, int rowNum) throws SQLException {
			User userInfo = new User();
			if (rs != null) {
				userInfo.setUserId(rs.getString("USER_CD"));
				userInfo.setUserName(rs.getString("USER_FIRST_NM") + " " + rs.getString("USER_FIRST_NM"));
				userInfo.setPartyId(rs.getInt("PARTY_ID"));
				userInfo.setPassword(rs.getString("USER_PASSWORD_CD"));
				userInfo.setRoleId(rs.getInt("ROLE_ID"));
			}

			return userInfo;
		}

	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

}