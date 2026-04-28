window.onload = function () {
    const faceRegisterStatus = sessionStorage.getItem('faceRegisterStatus');
    if (faceRegisterStatus === 'success') {
        document.getElementById('faceStatus').style.display = 'none';
        document.getElementById('faceSuccess').style.display = 'inline-block';
        const tempEmpData = JSON.parse(sessionStorage.getItem('tempEmployeeData') || '{}');
        if (tempEmpData.name) document.getElementById('empName').value = tempEmpData.name;
        if (tempEmpData.no) document.getElementById('empNo').value = tempEmpData.no;
        if (tempEmpData.password) document.getElementById('password').value = tempEmpData.password;
        if (tempEmpData.role) document.getElementById('empRole').value = tempEmpData.role;
    } else {
        sessionStorage.removeItem('faceRegisterStatus');
    }
};

function gotoFaceRegister() {
    const name = document.getElementById('empName').value.trim();
    const no = document.getElementById('empNo').value.trim();
    const password = document.getElementById('password').value.trim();
    const role = document.getElementById('empRole').value;

    if (!name) { alert('请输入姓名'); return; }
    if (!no) { alert('请输入工号'); return; }

    sessionStorage.setItem('tempEmployeeData', JSON.stringify({
        name, no, password, role, fromAddPage: true
    }));
    sessionStorage.removeItem('faceRegisterStatus');
    window.location.href = '../face/register_face.html';
}

function saveEmployee() {
    const userName = document.getElementById('empName').value.trim();
    const userId = document.getElementById('empNo').value.trim();
    const password = document.getElementById('password').value.trim();
    const userRole = document.getElementById('empRole').value;

    if (!userName) { alert('请输入员工姓名！'); return; }
    if (!userId) { alert('请输入员工工号！'); return; }
    if (!password) { alert('请输入登录密码！'); return; }

    const employeeData = {
        userName: userName,
        userId: userId,
        password: password,
        userRole: userRole,
        faceRegistered: document.getElementById('faceSuccess').style.display === 'inline-block'
    };

    fetch(API_BASE_URL + '/user/add', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(employeeData)
    })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                alert('新增员工成功！');
                sessionStorage.removeItem('tempEmployeeData');
                sessionStorage.removeItem('faceRegisterStatus');
                window.close();
            } else {
                let msg = data.message || '提交失败';
                const errorData = data.data;
                if (errorData && typeof errorData === 'object') {
                    try {
                        const firstField = Object.keys(errorData)[0];
                        const firstMsg = errorData[firstField];
                        msg += '，' + firstField + '：' + firstMsg;
                    } catch (e) {}
                }
                alert('新增失败：' + msg);
            }
        })
        .catch(error => {
            console.error('保存失败：', error);
            alert('新增员工失败，请稍后重试！' + error);
        });
}

function closePage() {
    sessionStorage.removeItem('tempEmployeeData');
    sessionStorage.removeItem('faceRegisterStatus');
    window.close();
}