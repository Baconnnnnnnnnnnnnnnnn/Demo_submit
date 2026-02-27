package com.example.pms.repository;

import com.example.pms.model.GroupInvitation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GroupInvitationRepository {
    @Autowired
    private JdbcTemplate db;

    public List<GroupInvitation> findPendingByGroup(int groupId) {
        try {
            String sql = "SELECT * FROM Group_Invitations WHERE GroupID = ? AND Status = 'PENDING' ORDER BY InvitedDate DESC";
            return db.query(sql, (rs, rn) -> {
                GroupInvitation inv = new GroupInvitation();
                inv.setInvitationId(rs.getInt("InvitationID"));
                inv.setGroupId(rs.getInt("GroupID"));
                inv.setStudentId(rs.getInt("StudentID"));
                inv.setInvitedByStudentId(rs.getInt("InvitedByStudentID"));
                inv.setStatus(rs.getString("Status"));

                java.sql.Timestamp ts1 = rs.getTimestamp("InvitedDate");
                if (ts1 != null) inv.setInvitedDate(ts1.toLocalDateTime());

                java.sql.Timestamp ts2 = rs.getTimestamp("RespondedDate");
                if (ts2 != null) inv.setRespondedDate(ts2.toLocalDateTime());

                return inv;
            }, groupId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    public List<GroupInvitation> findPendingByGroupAndStudent(int groupId, int studentId) {
        try {
            String sql = "SELECT * FROM Group_Invitations WHERE GroupID = ? AND StudentID = ? AND Status = 'PENDING'";
            return db.query(sql, (rs, rn) -> {
                GroupInvitation inv = new GroupInvitation();
                inv.setInvitationId(rs.getInt("InvitationID"));
                inv.setGroupId(rs.getInt("GroupID"));
                inv.setStudentId(rs.getInt("StudentID"));
                inv.setInvitedByStudentId(rs.getInt("InvitedByStudentID"));
                inv.setStatus(rs.getString("Status"));
                
                java.sql.Timestamp ts1 = rs.getTimestamp("InvitedDate");
                if (ts1 != null) inv.setInvitedDate(ts1.toLocalDateTime());
                
                java.sql.Timestamp ts2 = rs.getTimestamp("RespondedDate");
                if (ts2 != null) inv.setRespondedDate(ts2.toLocalDateTime());
                
                return inv;
            }, groupId, studentId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    public List<GroupInvitation> findPendingByStudent(int studentId) {
        try {
            String sql = "SELECT * FROM Group_Invitations WHERE StudentID = ? AND Status = 'PENDING' ORDER BY InvitedDate DESC";
            return db.query(sql, (rs, rn) -> {
                GroupInvitation inv = new GroupInvitation();
                inv.setInvitationId(rs.getInt("InvitationID"));
                inv.setGroupId(rs.getInt("GroupID"));
                inv.setStudentId(rs.getInt("StudentID"));
                inv.setInvitedByStudentId(rs.getInt("InvitedByStudentID"));
                inv.setStatus(rs.getString("Status"));
                
                java.sql.Timestamp ts1 = rs.getTimestamp("InvitedDate");
                if (ts1 != null) inv.setInvitedDate(ts1.toLocalDateTime());
                
                java.sql.Timestamp ts2 = rs.getTimestamp("RespondedDate");
                if (ts2 != null) inv.setRespondedDate(ts2.toLocalDateTime());
                
                return inv;
            }, studentId);
        } catch (Exception ex) {
            return List.of();
        }
    }

    public int create(int groupId, int studentId, int invitedByStudentId) {
        try {
            String sql = "INSERT INTO Group_Invitations (GroupID, StudentID, InvitedByStudentID, Status, InvitedDate) " +
                    "OUTPUT INSERTED.InvitationID VALUES (?, ?, ?, 'PENDING', GETDATE())";
            Integer invId = db.queryForObject(sql, Integer.class, groupId, studentId, invitedByStudentId);
            return invId != null ? invId : -1;
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public boolean existsPendingByGroupAndStudent(int groupId, int studentId) {
        try {
            String sql = "SELECT COUNT(*) FROM Group_Invitations WHERE GroupID = ? AND StudentID = ? AND Status = 'PENDING'";
            Integer count = db.queryForObject(sql, Integer.class, groupId, studentId);
            return count != null && count > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public int updateStatus(int invitationId, String status) {
        try {
            String sql = "UPDATE Group_Invitations SET Status = ?, RespondedDate = GETDATE() WHERE InvitationID = ?";
            return db.update(sql, status, invitationId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    public GroupInvitation findById(int invitationId) {
        try {
            String sql = "SELECT * FROM Group_Invitations WHERE InvitationID = ?";
            return db.queryForObject(sql, (rs, rn) -> {
                GroupInvitation inv = new GroupInvitation();
                inv.setInvitationId(rs.getInt("InvitationID"));
                inv.setGroupId(rs.getInt("GroupID"));
                inv.setStudentId(rs.getInt("StudentID"));
                inv.setInvitedByStudentId(rs.getInt("InvitedByStudentID"));
                inv.setStatus(rs.getString("Status"));
                java.sql.Timestamp ts1 = rs.getTimestamp("InvitedDate");
                if (ts1 != null) inv.setInvitedDate(ts1.toLocalDateTime());
                java.sql.Timestamp ts2 = rs.getTimestamp("RespondedDate");
                if (ts2 != null) inv.setRespondedDate(ts2.toLocalDateTime());
                return inv;
            }, invitationId);
        } catch (Exception ex) {
            return null;
        }
    }

    public int deleteByGroup(int groupId) {
        try {
            String sql = "DELETE FROM Group_Invitations WHERE GroupID = ?";
            return db.update(sql, groupId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }
}
