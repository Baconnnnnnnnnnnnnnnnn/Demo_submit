package root.controller;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/home")
@MultipartConfig(maxFileSize = 1024 * 1024 * 10) // Tăng lên 10MB
public class HomeController extends HttpServlet {
    
    // Class phụ để giữ thông tin file
	public static class FileEntry {
	    private String id;
	    private String name;
	    private String type;
	    private byte[] content;

	    public FileEntry(String name, String type, byte[] content) {
	        this.id = java.util.UUID.randomUUID().toString();
	        this.name = name;
	        this.type = type;
	        this.content = content;
	    }

	    // BẮT BUỘC phải có các hàm này để JSP đọc được
	    public String getId() { return id; }
	    public String getName() { return name; }
	    public String getType() { return type; }
	    public byte[] getContent() { return content; }
	}

    // Lưu trữ trong RAM (mất khi restart server)
    private static Map<String, FileEntry> fileMap = new LinkedHashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        // 1. Xử lý tải 1 file
        if ("download".equals(action)) {
            String fileId = request.getParameter("id");
            FileEntry file = fileMap.get(fileId);
            if (file != null) {
                response.setContentType(file.type);
                response.setHeader("Content-Disposition", "attachment; filename=\"" + file.name + "\"");
                response.getOutputStream().write(file.content);
                return;
            }
        }

        // 2. Xử lý tải nhiều file cùng lúc (ZIP)
        if ("downloadSelected".equals(action)) {
            String[] selectedIds = request.getParameterValues("selectedFiles");
            if (selectedIds != null && selectedIds.length > 0) {
                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=\"all_assignments.zip\"");
                
                try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                    for (String id : selectedIds) {
                        FileEntry file = fileMap.get(id);
                        if (file != null) {
                            zos.putNextEntry(new ZipEntry(file.name));
                            zos.write(file.content);
                            zos.closeEntry();
                        }
                    }
                }
                return;
            }
        }
        
        // 3. Xử lý xem trực tiếp file (nếu trình duyệt hỗ trợ)
        if ("view".equals(action)) {
            String fileId = request.getParameter("id");
            FileEntry file = fileMap.get(fileId);
            if (file != null) {
                // 1. Thiết lập đúng kiểu dữ liệu (MIME Type) để trình duyệt biết nó đang xem gì
                response.setContentType(file.getType()); 
                
                // 2. Sử dụng "inline" để trình duyệt mở trực tiếp
                response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
                
                // 3. Ghi dữ liệu ra luồng output
                response.getOutputStream().write(file.getContent());
                return;
            }
        }
        
        // Mặc định: Hiển thị trang chủ
        request.setAttribute("files", fileMap.values());
        request.getRequestDispatcher("/WEB-INF/view/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        Part filePart = request.getPart("assignment");
        if (filePart != null && filePart.getSize() > 0) {
            byte[] fileData = filePart.getInputStream().readAllBytes();
            FileEntry newFile = new FileEntry(filePart.getSubmittedFileName(), filePart.getContentType(), fileData);
            fileMap.put(newFile.id, newFile);
            request.setAttribute("msg", "Nộp bài thành công!");
        }
        
        doGet(request, response);
    }
}