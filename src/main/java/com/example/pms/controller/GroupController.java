package com.example.pms.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.pms.model.Classes;
import com.example.pms.model.Group;
import com.example.pms.model.GroupInvitation;
import com.example.pms.model.Semester;
import com.example.pms.model.Student;
import com.example.pms.repository.ClassRepository;
import com.example.pms.repository.GroupInvitationRepository;
import com.example.pms.repository.GroupMemberRepository;
import com.example.pms.repository.GroupRepository;
import com.example.pms.repository.SemesterRepository;
import com.example.pms.repository.StudentRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student/group")
public class GroupController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupInvitationRepository groupInvitationRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private StudentRepository studentRepository;

    @GetMapping("/list")
    public String listGroups(Model model, HttpSession session) {
        Student student = (Student) session.getAttribute("userProfile");
        if (student == null) {
            return "redirect:/acc/log";
        }

        int semesterId = getCurrentSemesterId();
        List<Group> groups = groupRepository.findByStudentAndSemester(student.getStudentId(), semesterId);

        addCommonPageAttributes(model, session, student);
        model.addAttribute("groups", groups);
        model.addAttribute("studentId", student.getStudentId());
        model.addAttribute("canCreateGroup", !groupRepository.hasActiveGroup(student.getStudentId(), semesterId));
        return "student/group/list";
    }

    @GetMapping("/create")
    public String createGroupForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Student student = (Student) session.getAttribute("userProfile");
        if (student == null) {
            return "redirect:/acc/log";
        }

        int semesterId = getCurrentSemesterId();
        if (groupRepository.hasActiveGroup(student.getStudentId(), semesterId)) {
            redirectAttributes.addFlashAttribute("error", "Ban da o trong 1 nhom, khong the tao nhom moi.");
            return "redirect:/student/group/list";
        }

        addCommonPageAttributes(model, session, student);
        return "student/group/create";
    }

    @PostMapping("/create")
    public String createGroup(@RequestParam("groupName") String groupName,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Student student = (Student) session.getAttribute("userProfile");
        if (student == null) {
            return "redirect:/acc/log";
        }

        String normalizedGroupName = groupName == null ? "" : groupName.trim();
        if (normalizedGroupName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ten nhom khong duoc de trong.");
            return "redirect:/student/group/create";
        }

        int semesterId = getCurrentSemesterId();
        if (groupRepository.hasActiveGroup(student.getStudentId(), semesterId)) {
            redirectAttributes.addFlashAttribute("error", "Ban da o trong 1 nhom, khong the tao nhom moi.");
            return "redirect:/student/group/list";
        }

        if (student.getClassId() == null) {
            redirectAttributes.addFlashAttribute("error", "Khong tim thay lop cua ban de tao nhom.");
            return "redirect:/student/group/list";
        }

        int groupId = groupRepository.create(normalizedGroupName, student.getClassId(), semesterId, student.getStudentId());
        if (groupId <= 0) {
            redirectAttributes.addFlashAttribute("error", "Tao nhom that bai.");
            return "redirect:/student/group/create";
        }

        int addResult = groupMemberRepository.addMember(groupId, student.getStudentId());
        if (addResult <= 0) {
            redirectAttributes.addFlashAttribute("error", "Tao nhom thanh cong nhung khong the them truong nhom.");
            return "redirect:/student/group/list";
        }

        redirectAttributes.addFlashAttribute("success", "Tao nhom thanh cong.");
        return "redirect:/student/group/" + groupId;
    }

    @GetMapping("/{id}")
    public String viewGroup(@PathVariable("id") int groupId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Student student = (Student) session.getAttribute("userProfile");
        if (student == null) {
            return "redirect:/acc/log";
        }

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            redirectAttributes.addFlashAttribute("error", "Nhom khong ton tai.");
            return "redirect:/student/group/list";
        }

        if (!groupMemberRepository.isMember(groupId, student.getStudentId())) {
            redirectAttributes.addFlashAttribute("error", "Ban khong phai thanh vien nhom nay.");
            return "redirect:/student/group/list";
        }

        boolean isLeader = group.getLeaderId() != null && group.getLeaderId() == student.getStudentId();
        List<Student> members = groupMemberRepository.findMemberDetailsOfGroup(groupId);
        int memberCount = groupMemberRepository.countMembers(groupId);
        List<GroupInvitation> pendingRequests = isLeader
                ? groupInvitationRepository.findPendingByGroup(groupId)
                : List.of();

        addCommonPageAttributes(model, session, student);
        model.addAttribute("group", group);
        model.addAttribute("members", members);
        model.addAttribute("isLeader", isLeader);
        model.addAttribute("studentId", student.getStudentId());
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("canDeleteGroup", isLeader && memberCount == 1);
        return "student/group/detail";
    }

    @PostMapping("/{id}/invite")
    public String inviteMember(@PathVariable("id") int groupId,
            @RequestParam("studentId") int targetId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Student me = (Student) session.getAttribute("userProfile");
        if (me == null) {
            return "redirect:/acc/log";
        }

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            redirectAttributes.addFlashAttribute("error", "Nhom khong ton tai.");
            return "redirect:/student/group/list";
        }

        if (!groupMemberRepository.isMember(groupId, me.getStudentId())) {
            redirectAttributes.addFlashAttribute("error", "Ban khong phai thanh vien nhom nay.");
            return "redirect:/student/group/list";
        }

        if (targetId == me.getStudentId()) {
            redirectAttributes.addFlashAttribute("error", "Khong the moi chinh ban vao nhom.");
            return "redirect:/student/group/" + groupId;
        }

        Student targetStudent = studentRepository.findById(targetId);
        if (targetStudent == null) {
            redirectAttributes.addFlashAttribute("error", "Khong tim thay sinh vien can moi.");
            return "redirect:/student/group/" + groupId;
        }

        if (targetStudent.getClassId() == null || targetStudent.getClassId() != group.getClassId()) {
            redirectAttributes.addFlashAttribute("error", "Chi duoc moi sinh vien cung lop.");
            return "redirect:/student/group/" + groupId;
        }

        if (groupMemberRepository.isMember(groupId, targetId)) {
            redirectAttributes.addFlashAttribute("error", "Sinh vien nay da o trong nhom.");
            return "redirect:/student/group/" + groupId;
        }

        if (groupRepository.hasActiveGroup(targetId, group.getSemesterId())) {
            redirectAttributes.addFlashAttribute("error", "Sinh vien nay da o trong nhom khac.");
            return "redirect:/student/group/" + groupId;
        }

        if (groupInvitationRepository.existsPendingByGroupAndStudent(groupId, targetId)) {
            redirectAttributes.addFlashAttribute("error", "Sinh vien nay da co yeu cau moi dang cho xu ly.");
            return "redirect:/student/group/" + groupId;
        }

        boolean isLeader = group.getLeaderId() != null && group.getLeaderId() == me.getStudentId();
        if (isLeader) {
            int added = groupMemberRepository.addMember(groupId, targetId);
            if (added > 0) {
                redirectAttributes.addFlashAttribute("success", "Moi thanh cong. Thanh vien da duoc them vao nhom.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Khong the them thanh vien vao nhom.");
            }
            return "redirect:/student/group/" + groupId;
        }

        int invitationId = groupInvitationRepository.create(groupId, targetId, me.getStudentId());
        if (invitationId > 0) {
            redirectAttributes.addFlashAttribute("success", "Da gui yeu cau moi den nhom truong de duyet.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Gui yeu cau moi that bai.");
        }
        return "redirect:/student/group/" + groupId;
    }

    @PostMapping("/{id}/invite/{invId}/review")
    public String reviewInviteRequest(@PathVariable("id") int groupId,
            @PathVariable("invId") int invitationId,
            @RequestParam("action") String action,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Student me = (Student) session.getAttribute("userProfile");
        if (me == null) {
            return "redirect:/acc/log";
        }

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            redirectAttributes.addFlashAttribute("error", "Nhom khong ton tai.");
            return "redirect:/student/group/list";
        }

        boolean isLeader = group.getLeaderId() != null && group.getLeaderId() == me.getStudentId();
        if (!isLeader) {
            redirectAttributes.addFlashAttribute("error", "Chi nhom truong moi duoc duyet yeu cau.");
            return "redirect:/student/group/" + groupId;
        }

        GroupInvitation invitation = groupInvitationRepository.findById(invitationId);
        if (invitation == null || invitation.getGroupId() != groupId) {
            redirectAttributes.addFlashAttribute("error", "Yeu cau moi khong hop le.");
            return "redirect:/student/group/" + groupId;
        }

        if (!"PENDING".equalsIgnoreCase(invitation.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Yeu cau moi da duoc xu ly.");
            return "redirect:/student/group/" + groupId;
        }

        if ("approve".equalsIgnoreCase(action)) {
            if (groupRepository.hasActiveGroup(invitation.getStudentId(), group.getSemesterId())) {
                groupInvitationRepository.updateStatus(invitationId, "REJECTED");
                redirectAttributes.addFlashAttribute("error", "Sinh vien da vao nhom khac. Yeu cau duoc tu choi.");
                return "redirect:/student/group/" + groupId;
            }

            if (!groupMemberRepository.isMember(groupId, invitation.getStudentId())) {
                int addResult = groupMemberRepository.addMember(groupId, invitation.getStudentId());
                if (addResult <= 0) {
                    redirectAttributes.addFlashAttribute("error", "Khong the them thanh vien sau khi duyet.");
                    return "redirect:/student/group/" + groupId;
                }
            }

            groupInvitationRepository.updateStatus(invitationId, "ACCEPTED");
            redirectAttributes.addFlashAttribute("success", "Da duyet yeu cau moi.");
            return "redirect:/student/group/" + groupId;
        }

        groupInvitationRepository.updateStatus(invitationId, "REJECTED");
        redirectAttributes.addFlashAttribute("success", "Da tu choi yeu cau moi.");
        return "redirect:/student/group/" + groupId;
    }

    @PostMapping("/{id}/kick")
    public String kickMember(@PathVariable("id") int groupId,
            @RequestParam("studentId") int targetId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Student me = (Student) session.getAttribute("userProfile");
        if (me == null) {
            return "redirect:/acc/log";
        }

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            redirectAttributes.addFlashAttribute("error", "Nhom khong ton tai.");
            return "redirect:/student/group/list";
        }

        boolean isLeader = group.getLeaderId() != null && group.getLeaderId() == me.getStudentId();
        if (!isLeader) {
            redirectAttributes.addFlashAttribute("error", "Chi nhom truong moi duoc kick thanh vien.");
            return "redirect:/student/group/" + groupId;
        }

        if (group.getLeaderId() != null && targetId == group.getLeaderId()) {
            redirectAttributes.addFlashAttribute("error", "Nhom truong khong the tu kick chinh minh.");
            return "redirect:/student/group/" + groupId;
        }

        if (!groupMemberRepository.isMember(groupId, targetId)) {
            redirectAttributes.addFlashAttribute("error", "Sinh vien nay khong o trong nhom.");
            return "redirect:/student/group/" + groupId;
        }

        int removed = groupMemberRepository.removeMember(groupId, targetId);
        if (removed > 0) {
            redirectAttributes.addFlashAttribute("success", "Da kick thanh vien ra khoi nhom.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Kick thanh vien that bai.");
        }
        return "redirect:/student/group/" + groupId;
    }

    @PostMapping("/{id}/leave")
    public String leaveGroup(@PathVariable("id") int groupId, HttpSession session, RedirectAttributes redirectAttributes) {
        Student me = (Student) session.getAttribute("userProfile");
        if (me == null) {
            return "redirect:/acc/log";
        }

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            redirectAttributes.addFlashAttribute("error", "Nhom khong ton tai.");
            return "redirect:/student/group/list";
        }

        if (!groupMemberRepository.isMember(groupId, me.getStudentId())) {
            redirectAttributes.addFlashAttribute("error", "Ban khong phai thanh vien nhom nay.");
            return "redirect:/student/group/list";
        }

        boolean isLeader = group.getLeaderId() != null && group.getLeaderId() == me.getStudentId();
        if (isLeader) {
            redirectAttributes.addFlashAttribute("error", "Nhom truong khong the roi nhom. Hay xoa nhom khi chi con 1 minh ban.");
            return "redirect:/student/group/" + groupId;
        }

        int removed = groupMemberRepository.removeMember(groupId, me.getStudentId());
        if (removed > 0) {
            redirectAttributes.addFlashAttribute("success", "Ban da roi nhom.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Khong the roi nhom.");
        }
        return "redirect:/student/group/list";
    }

    @PostMapping("/{id}/delete")
    public String deleteGroup(@PathVariable("id") int groupId, HttpSession session, RedirectAttributes redirectAttributes) {
        Student me = (Student) session.getAttribute("userProfile");
        if (me == null) {
            return "redirect:/acc/log";
        }

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            redirectAttributes.addFlashAttribute("error", "Nhom khong ton tai.");
            return "redirect:/student/group/list";
        }

        boolean isLeader = group.getLeaderId() != null && group.getLeaderId() == me.getStudentId();
        if (!isLeader) {
            redirectAttributes.addFlashAttribute("error", "Chi nhom truong moi duoc xoa nhom.");
            return "redirect:/student/group/" + groupId;
        }

        int memberCount = groupMemberRepository.countMembers(groupId);
        if (memberCount > 1) {
            redirectAttributes.addFlashAttribute("error", "Chi duoc xoa nhom khi nhom chi con nhom truong.");
            return "redirect:/student/group/" + groupId;
        }

        groupInvitationRepository.deleteByGroup(groupId);
        groupMemberRepository.removeByGroup(groupId);
        int deleted = groupRepository.deleteGroup(groupId);
        if (deleted > 0) {
            redirectAttributes.addFlashAttribute("success", "Da xoa nhom.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Xoa nhom that bai.");
        }
        return "redirect:/student/group/list";
    }

    private int getCurrentSemesterId() {
        Semester semester = semesterRepository.findCurrentSemester();
        if (semester == null) {
            semester = semesterRepository.findById(1);
        }
        return semester != null ? semester.getSemesterId() : 1;
    }

    private String resolveClassName(Student student) {
        if (student == null || student.getClassId() == null) {
            return "PMS";
        }
        Classes classObj = classRepository.findById(student.getClassId());
        return classObj != null ? classObj.getClassName() : "PMS";
    }

    private void addCommonPageAttributes(Model model, HttpSession session, Student student) {
        Object fullName = session.getAttribute("fullName");
        Object role = session.getAttribute("role");
        model.addAttribute("studentName",
                fullName != null ? fullName : (student != null ? student.getFullName() : "Hoc sinh"));
        model.addAttribute("userRole", role != null ? role : "Hoc sinh");
        model.addAttribute("className", resolveClassName(student));
    }
}
