let currentEditData = null;
let employeePageNum = 1;
let employeeTotal = 0;

window.onload = function () {
    loadEmployeeList(document.getElementById('searchInput').value);
    window.addEventListener('focus', function () {
        loadEmployeeList(document.getElementById('searchInput').value);
    });

    const recentFaceEmpNo = sessionStorage.getItem('recentFaceRegisterEmpNo');
    if (recentFaceEmpNo) {
        alert('工号' + recentFaceEmpNo + '的员工人脸录入成功！');
        sessionStorage.removeItem('recentFaceRegisterEmpNo');
    }
};

function openAddEmployeePage() {
    sessionStorage.removeItem('tempEmployeeData');
    sessionStorage.removeItem('faceRegisterStatus');
    window.open('add_employee.html', '_blank');
}

function loadEmployeeList(pageNum) {
    if (pageNum) employeePageNum = pageNum;
    const tableBody = document.getElementById('employeeTableBody');
    tableBody.innerHTML = '<tr><td colspan="7" style="text-align: center;">加载中...</td></tr>';

    const searchKeyword = document.getElementById('searchInput').value.trim();
    const pageSize = 15;
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);

    fetch(API_BASE_URL + '/user/query-user-list-paged?keyword=' + encodeURIComponent(searchKeyword || '') + '&pageNum=' + employeePageNum + '&pageSize=' + pageSize, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [AUTH_HEADER]: token
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('网络请求失败');
            return response.json();
        })
        .then(data => {
            if (data.code === 200 && data.data && data.data.list) {
                employeeTotal = data.data.total;
                renderEmployeeTable(data.data.list);
                renderEmployeePagination();
            } else {
                tableBody.innerHTML = '<tr><td colspan="7" style="text-align: center;">暂无员工数据</td></tr>';
                document.getElementById('paginationContainer').innerHTML = '';
            }
        })
        .catch(error => {
            console.error('加载员工列表失败：', error);
            tableBody.innerHTML = '<tr><td colspan="7" style="text-align: center;">加载失败，请刷新重试</td></tr>';
        });
}

function renderEmployeePagination() {
    const container = document.getElementById('paginationContainer');
    const totalPages = Math.ceil(employeeTotal / 15);
    container.innerHTML = `
        <button class="btn btn-default btn-sm" onclick="loadEmployeeList(document.getElementById('searchInput').value, ${employeePageNum - 1})" ${employeePageNum <= 1 ? 'disabled' : ''}>上一页</button>
        <span style="margin: 0 5px;">第</span>
        <input type="number" id="employeePageInput" value="${employeePageNum}" min="1" max="${totalPages}" style="width: 50px; text-align: center; padding: 2px;" onkeypress="if(event.key==='Enter'){let p=parseInt(this.value);if(p>=1&&p<=${totalPages})loadEmployeeList(document.getElementById('searchInput').value, p);}">
        <span style="margin: 0 5px;">/ ${totalPages} 页，共 ${employeeTotal} 条</span>
        <button class="btn btn-default btn-sm" onclick="loadEmployeeList(document.getElementById('searchInput').value, ${employeePageNum + 1})" ${employeePageNum >= totalPages ? 'disabled' : ''}>下一页</button>
    `;
}

function renderEmployeeTable(employeeList) {
    const tableBody = document.getElementById('employeeTableBody');
    if (employeeList.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" style="text-align: center;">暂无员工数据</td></tr>';
        return;
    }

    let tableHtml = '';
    employeeList.forEach(emp => {
        const roleText = emp.userRole === 0 ? '管理员' : '普通员工';
        const statusText = emp.status === 1 ? '正常' : '禁用';
        const faceStatusHtml = emp.faceRegistered
            ? '<span class="status-success">已录入</span>'
            : '<span class="status-warning">未录入</span>';
        const fingerStatusHtml = emp.fingerRegistered
            ? '<span class="status-success">已录入</span>'
            : '<span class="status-warning">未录入</span>';

        const faceBtn = emp.faceRegistered
            ? `<button class="btn btn-danger btn-sm" onclick="deleteFaceData('${emp.userName}', '${emp.userId}')">删除人脸</button>`
            : `<button class="btn btn-primary btn-sm" onclick="gotoFaceRegister('${emp.userName}', '${emp.userId}', '${emp.faceRegistered}', 'face')">录入人脸</button>`;

        const fingerBtn = emp.fingerRegistered
            ? `<button class="btn btn-danger btn-sm" onclick="gotoFingerRegister('${emp.userName}', '${emp.userId}', '${emp.fingerRegistered}')">删除指纹</button>`
            : `<button class="btn btn-primary btn-sm" onclick="gotoFingerRegister('${emp.userName}', '${emp.userId}', '${emp.fingerRegistered}')">录入指纹</button>`;

        tableHtml += `
        <tr>
            <td>${emp.userName || '-'}</td>
            <td>${emp.userId || '-'}</td>
            <td>${roleText}</td>
            <td>${statusText}</td>
            <td>${faceStatusHtml}</td>
            <td>${fingerStatusHtml}</td>
            <td>
                <button class="btn btn-default btn-sm" onclick="editEmployee('${emp.userId}')">编辑</button>
                <button class="btn btn-danger btn-sm" onclick="deleteEmployee('${emp.userId}')">删除</button>
                ${faceBtn}
                ${fingerBtn}
            </td>
        </tr>`;
    });

    tableBody.innerHTML = tableHtml;
}

function gotoFaceRegister(name, no, faceRegistered, type) {
    if (!name || !no) {
        alert('员工信息异常');
        return;
    }
    sessionStorage.setItem('tempEmployeeData', JSON.stringify({
        name, no, fromAddPage: false
    }));
    sessionStorage.removeItem('faceRegisterStatus');
    window.location.href = '../face/register_face.html';
}

function gotoFingerRegister(name, no, fingerRegistered) {
    if (!name || !no) {
        alert('员工信息异常');
        return;
    }
    window.location.href = '../face/enroll_fingerprint.html?userId=' + encodeURIComponent(no) + '&userName=' + encodeURIComponent(name) + '&fingerRegistered=' + encodeURIComponent(fingerRegistered || 'N');
}

function editEmployee(empNo) {
    const tableBody = document.getElementById('employeeTableBody');
    const rows = tableBody.querySelectorAll('tr');

    for (let row of rows) {
        const cells = row.querySelectorAll('td');
        if (cells.length >= 5 && cells[1].textContent.trim() === empNo) {
            const userName = cells[0].textContent.trim();
            const roleText = cells[2].textContent.trim();
            const statusText = cells[3].textContent.trim();

            currentEditData = {
                userId: empNo,
                userName: userName,
                userRole: roleText === '管理员' ? 0 : 1,
                status: statusText === '正常' ? 1 : 0
            };

            document.getElementById('editUserId').value = empNo;
            document.getElementById('editUserName').value = userName;
            document.getElementById('editUserIdDisplay').value = empNo;
            document.getElementById('editPassword').value = '';
            document.getElementById('editUserRole').value = currentEditData.userRole;
            document.getElementById('editStatus').value = currentEditData.status;

            document.getElementById('editModal').style.display = 'flex';
            return;
        }
    }
    alert('未找到该员工信息');
}

function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
    currentEditData = null;
}

function saveEditEmployee() {
    const userId = document.getElementById('editUserId').value;
    const userName = document.getElementById('editUserName').value.trim();
    const password = document.getElementById('editPassword').value;
    const userRole = parseInt(document.getElementById('editUserRole').value);
    const status = parseInt(document.getElementById('editStatus').value);

    if (!userName) {
        alert('请输入员工姓名');
        return;
    }

    const userData = {
        userId: userId,
        userName: userName,
        userRole: userRole,
        status: status
    };

    if (password) {
        userData.password = password;
    }

    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);

    fetch(API_BASE_URL + '/user/update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [AUTH_HEADER]: token
        },
        body: JSON.stringify(userData)
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('更新成功');
                closeEditModal();
                loadEmployeeList(document.getElementById('searchInput').value);
            } else {
                alert('更新失败：' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('更新失败：', error);
            alert('更新失败，请稍后重试');
        });
}

function deleteEmployee(empNo) {
    if (!confirm('确定要删除工号为' + empNo + '的员工吗？')) {
        return;
    }

    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);

    fetch(API_BASE_URL + '/user/delete?userId=' + encodeURIComponent(empNo), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [AUTH_HEADER]: token
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('删除成功');
                loadEmployeeList(document.getElementById('searchInput').value);
            } else {
                alert('删除失败：' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('删除失败：', error);
            alert('删除失败，请稍后重试');
        });
}

function deleteFaceData(name, no) {
    if (!confirm('确定要删除员工"' + name + '"（工号：' + no + '）的人脸数据吗？\n删除后可重新进行人脸采集。')) {
        return;
    }
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    fetch(API_BASE_URL + '/face/delete', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [AUTH_HEADER]: token
        },
        body: JSON.stringify({userId: no})
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('人脸数据已删除，可以重新采集了');
                loadEmployeeList(document.getElementById('searchInput').value);
            } else {
                alert('删除失败：' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('删除失败：', error);
            alert('删除失败，请稍后重试');
        });
}