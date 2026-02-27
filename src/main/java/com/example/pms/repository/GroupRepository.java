package com.example.pms.repository;

import com.example.pms.model.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GroupRepository {
    @Autowired
    private JdbcTemplate db;

    public List<Group> findByStudentAndSemester(int studentId, int semesterId) {
        try {
            String sql = "SELECT DISTINCT g.* FROM Groups g " +
                    "INNER JOIN Group_Members gm ON g.GroupID = gm.GroupID " +
                    "WHERE gm.StudentID = ? AND g.SemesterID = ? AND gm.IsActive = 1 AND g.IsLocked = 0 " +
                    "ORDER BY g.CreatedDate DESC";
            return db.query(sql, (rs, rn) -> {
                Group g = new Group();
                g.setGroupId(rs.getInt("GroupID"));
                g.setGroupName(rs.getString("GroupName"));
                g.setClassId(rs.getInt("ClassID"));
                g.setSemesterId(rs.getInt("SemesterID"));
                
                int leaderId = rs.getInt("LeaderID");
                if (!rs.wasNull()) {
                    g.setLeaderId(leaderId);
                }
                
                java.sql.Timestamp ts = rs.getTimestamp("CreatedDate");
                if (ts != null) {
                    g.setCreatedDate(ts.toLocalDateTime());
                }
                
                g.setLocked(rs.getBoolean("IsLocked"));
                return g;
            }, studentId, semesterId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    public Group findById(int groupId) {
        try {
            String sql = "SELECT * FROM Groups WHERE GroupID = ?";
            return db.queryForObject(sql, (rs, rn) -> {
                Group g = new Group();
                g.setGroupId(rs.getInt("GroupID"));
                g.setGroupName(rs.getString("GroupName"));
                g.setClassId(rs.getInt("ClassID"));
                g.setSemesterId(rs.getInt("SemesterID"));
                int leaderId = rs.getInt("LeaderID");
                if (!rs.wasNull()) g.setLeaderId(leaderId);
                g.setCreatedDate(rs.getTimestamp("CreatedDate").toLocalDateTime());
                g.setLocked(rs.getBoolean("IsLocked"));
                return g;
            }, groupId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public int create(String groupName, int classId, int semesterId, int leaderId) {
        try {
            String sql = "INSERT INTO Groups (GroupName, ClassID, SemesterID, LeaderID, CreatedDate, IsLocked) " +
                    "OUTPUT INSERTED.GroupID VALUES (?, ?, ?, ?, GETDATE(), 0)";
            Integer groupId = db.queryForObject(sql, Integer.class, groupName, classId, semesterId, leaderId);
            return groupId != null ? groupId : -1;
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public boolean hasActiveGroup(int studentId, int semesterId) {
        try {
            String sql = "SELECT COUNT(*) FROM Group_Members gm " +
                    "INNER JOIN Groups g ON gm.GroupID = g.GroupID " +
                    "WHERE gm.StudentID = ? AND g.SemesterID = ? AND gm.IsActive = 1 AND g.IsLocked = 0";
            Integer count = db.queryForObject(sql, Integer.class, studentId, semesterId);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public int deleteGroup(int groupId) {
        try {
            String sql = "DELETE FROM Groups WHERE GroupID = ?";
            return db.update(sql, groupId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }
}
