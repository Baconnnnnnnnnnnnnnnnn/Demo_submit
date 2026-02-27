package com.example.pms.repository;

import com.example.pms.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StudentRepository {
    @Autowired
    private JdbcTemplate db;

    public Student findById(int studentId) {
        try {
            String sql = "SELECT * FROM Students WHERE StudentID = ?";
            return db.queryForObject(sql, (rs, rn) -> {
                Student s = new Student();
                s.setStudentId(rs.getInt("StudentID"));
                s.setStudentCode(rs.getString("StudentCode"));
                s.setFullName(rs.getString("FullName"));
                s.setSchoolEmail(rs.getString("SchoolEmail"));
                s.setPhoneNumber(rs.getString("PhoneNumber"));
                int cid = rs.getInt("ClassID");
                if (rs.wasNull()) s.setClassId(null); else s.setClassId(cid);
                int accId = rs.getInt("AccountID");
                if (rs.wasNull()) s.setAccountId(null); else s.setAccountId(accId);
                return s;
            }, studentId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public Student findBySchoolEmail(String email){
        try{
            String sql = "SELECT * FROM Students WHERE SchoolEmail = ?";
            return db.queryForObject(sql, (rs, rn) -> {
                Student s = new Student();
                s.setStudentId(rs.getInt("StudentID"));
                s.setStudentCode(rs.getString("StudentCode"));
                s.setFullName(rs.getString("FullName"));
                s.setSchoolEmail(rs.getString("SchoolEmail"));
                s.setPhoneNumber(rs.getString("PhoneNumber"));
                int cid = rs.getInt("ClassID"); if(rs.wasNull()) s.setClassId(null); else s.setClassId(cid);
                int accId = rs.getInt("AccountID"); if(rs.wasNull()) s.setAccountId(null); else s.setAccountId(accId);
                return s;
            }, email);
        }catch(EmptyResultDataAccessException ex){
            return null;
        }
    }
}
