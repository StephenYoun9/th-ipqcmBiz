let toolList = [];
let currentEditTool = null;
let selectedImageFile = null;
let existingImageUrl = null;

function openAddToolModal() {
    currentEditTool = null;
    selectedImageFile = null;
    existingImageUrl = null;
    document.getElementById('toolName').value = '';
    document.getElementById('toolCode').value = '';
    document.getElementById('cabinetNo').value = 'A01';
    document.getElementById('toolType').value = 'hand';
    document.querySelector('#addToolModal h3').textContent = '上架新工具';
    document.getElementById('statusGroup').style.display = 'none';
    document.getElementById('toolCode').readOnly = false;
    document.getElementById('toolImage').value = '';
    document.getElementById('imageName').textContent = '';
    document.getElementById('imagePreview').style.display = 'none';
    document.getElementById('addToolModal').style.display = 'flex';
}

function openEditToolModal(tool) {
    currentEditTool = tool;
    selectedImageFile = null;
    existingImageUrl = tool.imageUrl;
    document.querySelector('#addToolModal h3').textContent = '编辑工具';
    document.getElementById('toolName').value = tool.toolName;
    document.getElementById('toolCode').value = tool.toolCode;
    document.getElementById('toolCode').readOnly = true;
    document.getElementById('cabinetNo').value = tool.cabinetNo;
    document.getElementById('toolType').value = tool.toolType;
    document.getElementById('toolStatus').value = tool.status;
    document.getElementById('statusGroup').style.display = 'block';
    document.getElementById('toolImage').value = '';
    document.getElementById('imageName').textContent = '';

    if (tool.imageUrl) {
        document.getElementById('previewImg').src = API_BASE_URL + tool.imageUrl;
        document.getElementById('imagePreview').style.display = 'block';
    } else {
        document.getElementById('imagePreview').style.display = 'none';
    }

    document.getElementById('addToolModal').style.display = 'flex';
}

function closeAddToolModal() {
    document.getElementById('addToolModal').style.display = 'none';
    document.getElementById('toolCode').readOnly = false;
    document.getElementById('statusGroup').style.display = 'none';
    selectedImageFile = null;
    existingImageUrl = null;
}

function handleImageSelect(input) {
    if (input.files && input.files[0]) {
        selectedImageFile = input.files[0];
        document.getElementById('imageName').textContent = selectedImageFile.name;

        const reader = new FileReader();
        reader.onload = function(e) {
            document.getElementById('previewImg').src = e.target.result;
            document.getElementById('imagePreview').style.display = 'block';
        };
        reader.readAsDataURL(selectedImageFile);
    }
}

function removeImage() {
    selectedImageFile = null;
    document.getElementById('toolImage').value = '';
    document.getElementById('imageName').textContent = '';

    if (existingImageUrl) {
        document.getElementById('previewImg').src = API_BASE_URL + existingImageUrl;
        document.getElementById('imagePreview').style.display = 'block';
    } else {
        document.getElementById('imagePreview').style.display = 'none';
    }
}

function loadToolList(keyword) {
    const token = localStorage.getItem('token');
    fetch(API_BASE_URL + '/admin/tool/list?keyword=' + (keyword || ''), {
        headers: { 'Authorization': 'Bearer ' + token }
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            toolList = data.data || [];
            renderToolTable();
        }
    });
}

function renderToolTable() {
    const tbody = document.getElementById('toolTableBody');
    if (!tbody) return;

    if (toolList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = toolList.map(tool => {
        const statusMap = {
            'AVAILABLE': '可借',
            'BORROWED': '已借出',
            'MAINTENANCE': '维护中',
            'OFFLINE': '已下架'
        };
        const statusText = statusMap[tool.status] || tool.status;
        const statusClass = tool.status === 'AVAILABLE' ? 'status-available' :
                           tool.status === 'MAINTENANCE' ? 'status-maintenance' : '';

        let actionButtons = '';
        if (tool.status === 'MAINTENANCE') {
            actionButtons = `<button class="btn btn-success btn-sm" onclick="finishMaintenance('${tool.toolCode}')">完成维护</button>`;
        } else if (tool.status !== 'OFFLINE') {
            actionButtons = `<button class="btn btn-warning btn-sm" onclick="startMaintenance('${tool.toolCode}')">维护</button>`;
        }

        return `<tr>
            <td>${tool.toolName}</td>
            <td>${tool.toolCode}</td>
            <td>${tool.cabinetNo}</td>
            <td>${getToolTypeName(tool.toolType)}</td>
            <td><span class="status-badge ${statusClass}">${statusText}</span></td>
            <td>
                <button class="btn btn-default btn-sm" onclick="openEditToolModal(toolList.find(t=>t.toolCode==='${tool.toolCode}'))">编辑</button>
                <button class="btn btn-danger btn-sm" onclick="offlineTool('${tool.toolCode}')">下架</button>
                ${actionButtons}
            </td>
        </tr>`;
    }).join('');
}

function getToolTypeName(type) {
    const typeMap = {
        'hand': '手动工具',
        'electric': '电动工具',
        'measuring': '测量工具'
    };
    return typeMap[type] || type;
}

function saveTool() {
    const token = localStorage.getItem('token');
    const isEdit = currentEditTool !== null;
    const method = isEdit ? 'PUT' : 'POST';
    const url = isEdit ? API_BASE_URL + '/admin/tool' : API_BASE_URL + '/admin/tool';

    const toolCode = document.getElementById('toolCode').value;
    const toolName = document.getElementById('toolName').value;
    const cabinetNo = document.getElementById('cabinetNo').value;
    const toolType = document.getElementById('toolType').value;
    const status = document.getElementById('toolStatus').value;

    if (!toolName || !toolCode) {
        alert('请填写必填项');
        return;
    }

    const data = {
        toolCode: toolCode,
        toolName: toolName,
        cabinetNo: cabinetNo,
        toolType: toolType,
        status: status,
        imageUrl: existingImageUrl
    };

    fetch(url, {
        method: method,
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            if (selectedImageFile) {
                uploadImage(toolCode, isEdit);
            } else {
                alert(isEdit ? '更新成功' : '上架成功');
                closeAddToolModal();
                loadToolList();
            }
        } else {
            alert(data.message || '操作失败');
        }
    });
}

function uploadImage(toolCode, isEdit) {
    const token = localStorage.getItem('token');
    const formData = new FormData();
    formData.append('file', selectedImageFile);

    fetch(API_BASE_URL + '/admin/tool/image/' + toolCode, {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + token
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            existingImageUrl = data.data;
            alert(isEdit ? '更新成功' : '上架成功');
            closeAddToolModal();
            loadToolList();
        } else {
            alert((isEdit ? '更新成功但' : '上架成功但') + '图片上传失败: ' + data.message);
            closeAddToolModal();
            loadToolList();
        }
    })
    .catch(err => {
        alert(isEdit ? '更新成功但图片上传失败' : '上架成功但图片上传失败');
        closeAddToolModal();
        loadToolList();
    });
}

function deleteToolImage(toolCode) {
    if (!confirm('确定要删除该工具的图片吗？')) return;

    const token = localStorage.getItem('token');
    fetch(API_BASE_URL + '/admin/tool/image/' + toolCode, {
        method: 'DELETE',
        headers: { 'Authorization': 'Bearer ' + token }
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            alert('图片已删除');
            loadToolList();
        } else {
            alert(data.message || '删除失败');
        }
    });
}

function offlineTool(toolCode) {
    if (!confirm('确定要下架该工具吗？')) return;
    updateToolStatus(toolCode, 'OFFLINE');
}

function startMaintenance(toolCode) {
    if (!confirm('确定要将该工具设为维护中吗？')) return;
    updateToolStatus(toolCode, 'MAINTENANCE');
}

function finishMaintenance(toolCode) {
    if (!confirm('确定要完成维护吗？')) return;
    updateToolStatus(toolCode, 'AVAILABLE');
}

function updateToolStatus(toolCode, status) {
    const token = localStorage.getItem('token');
    fetch(API_BASE_URL + '/admin/tool/status/' + toolCode + '?status=' + status, {
        method: 'PUT',
        headers: { 'Authorization': 'Bearer ' + token }
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            alert('操作成功');
            loadToolList();
        } else {
            alert(data.message || '操作失败');
        }
    });
}

function searchTool() {
    const keyword = document.getElementById('searchKeyword').value;
    loadToolList(keyword);
}

document.addEventListener('DOMContentLoaded', function() {
    loadToolList();

    const searchInput = document.getElementById('searchKeyword');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchTool();
            }
        });
    }
});