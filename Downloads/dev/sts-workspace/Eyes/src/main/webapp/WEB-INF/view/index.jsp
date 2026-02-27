<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
<head>
<title>Hệ thống nộp bài</title>
<style>
body {
	font-family: 'Segoe UI', sans-serif;
	margin: 20px;
	background: #eee;
}

.card {
	background: white;
	padding: 20px;
	border-radius: 10px;
	box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

table {
	width: 100%;
	border-collapse: collapse;
	margin-top: 15px;
}

th, td {
	border: 1px solid #ddd;
	padding: 12px;
	text-align: left;
}

th {
	background-color: #f8f9fa;
}

.btn {
	padding: 8px 15px;
	text-decoration: none;
	border-radius: 5px;
	color: white;
	border: none;
	cursor: pointer;
}

.btn-download {
	background: #27ae60;
	font-size: 12px;
}

.btn-bulk {
	background: #e67e22;
	margin-bottom: 10px;
}

.student-zone {
	border-left: 5px solid #2ecc71;
	padding-left: 15px;
}

.teacher-zone {
	border-left: 5px solid #3498db;
	padding-left: 15px;
}
</style>
</head>
<body>
	<div class="card">
		<div style="margin-bottom: 20px;">
			<a href="?role=student" class="btn" style="background: #2ecc71">Học
				sinh</a> <a href="?role=teacher" class="btn" style="background: #3498db">Giáo
				viên</a>
		</div>

		<c:if test="${param.role == 'student'}">
			<div class="student-zone">
				<h3>Nộp bài tập</h3>
				<form action="home?role=student" method="post"
					enctype="multipart/form-data">
					<input type="file" name="assignment" required>
					<button type="submit" class="btn" style="background: #2ecc71">Tải
						lên</button>
				</form>
				<p style="color: green">${msg}</p>
			</div>
		</c:if>

		<c:if test="${param.role == 'teacher'}">
			<div class="teacher-zone">
				<h3>Danh sách bài nộp</h3>

				<form action="home" method="get">
					<input type="hidden" name="action" value="downloadSelected">
					<button type="submit" class="btn btn-bulk">Tải các file đã
						chọn (.zip)</button>

					<table>
						<thead>
							<tr>
								<th><input type="checkbox" onclick="toggleAll(this)"></th>
								<th>Tên file</th>
								<th>Định dạng</th>
								<th>Hành động</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach var="file" items="${files}">
								<tr>
									<td><input type="checkbox" name="selectedFiles"
										value="${file.id}"></td>
									<td>${file.name}</td>
									<td>${file.type}</td>
									<td><a href="home?action=view&id=${file.id}"
										target="_blank" class="btn"
										style="background: #9b59b6; margin-right: 5px;">Xem nhanh</a>

										<a href="home?action=download&id=${file.id}"
										class="btn btn-download">Tải xuống</a></td>
								</tr>
							</c:forEach>
						</tbody>
					</table>
				</form>
			</div>
		</c:if>
	</div>

	<script>
		function toggleAll(source) {
			checkboxes = document.getElementsByName('selectedFiles');
			for (var i = 0, n = checkboxes.length; i < n; i++) {
				checkboxes[i].checked = source.checked;
			}
		}
	</script>
</body>
</html>