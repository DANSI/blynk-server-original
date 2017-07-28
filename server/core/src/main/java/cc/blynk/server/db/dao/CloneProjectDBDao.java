package cc.blynk.server.db.dao;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.16.
 */
public class CloneProjectDBDao {

    public static final String selectClonedProjectByToken = "SELECT * from cloned_projects where token = ?";
    public static final String insertClonedProject = "INSERT INTO cloned_projects (token, ts, json) values (?, NOW(), ?)";

    private static final Logger log = LogManager.getLogger(CloneProjectDBDao.class);
    private final HikariDataSource ds;

    public CloneProjectDBDao(HikariDataSource ds) {
        this.ds = ds;
    }

    public void insertClonedProject(String token, String projectJson) throws Exception {
        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertClonedProject)) {

            ps.setString(1, token);
            ps.setString(2, projectJson);
            ps.executeUpdate();

            connection.commit();
        }
    }

    public String selectClonedProjectByToken(String token) throws Exception {
        try (Connection connection = ds.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectClonedProjectByToken)) {

            statement.setString(1, token);
            try (ResultSet rs = statement.executeQuery()) {
                connection.commit();
                if (rs.next()) {
                    return rs.getString("json");
                }
            }
        }
        return null;
    }

}
