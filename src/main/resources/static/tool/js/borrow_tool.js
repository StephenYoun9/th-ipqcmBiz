let selectedToolCode = null;
let toolPageNum = 1;
let toolTotal = 0;
let currentKeyword = '';
let currentToolType = '';

function getToolTypeName(type) {
    const typeMap = {
        'hand': '手动工具',
        'electric': '电动工具',
        'measuring': '测量工具'
    };
    return typeMap[type] || type;
}

function loadAvailableTools(pageNum, keyword, toolType) {
    if (pageNum) toolPageNum = pageNum;
    if (keyword !== undefined) currentKeyword = keyword;
    if (toolType !== undefined) currentToolType = toolType;

    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    const pageSize = 10;
    let url = API_BASE_URL + '/employee/tool/available?pageNum=' + toolPageNum + '&pageSize=' + pageSize;
    if (currentKeyword) url += '&keyword=' + encodeURIComponent(currentKeyword);
    if (currentToolType) url += '&toolType=' + encodeURIComponent(currentToolType);

    fetch(url, {
        method: 'GET',
        headers: {
            [AUTH_HEADER]: token,
            'Content-Type': 'application/json'
        }
    })
    .then(res => res.json())
    .then(data => {
        const tbody = document.getElementById('toolTableBody');
        if (data.code === 200 && data.data && data.data.list && data.data.list.length > 0) {
            toolTotal = data.data.total;
            let html = '';
            data.data.list.forEach(tool => {
                html += `<tr>
                    <td><input type="radio" name="tool" value="${tool.toolCode}" onchange="selectTool('${tool.toolCode}')"></td>
                    <td>${tool.toolName || '-'}</td>
                    <td>${tool.toolCode}</td>
                    <td>${tool.cabinetNo || '-'}</td>
                    <td>${getToolTypeName(tool.toolType)}</td>
                </tr>`;
            });
            tbody.innerHTML = html;
            renderPagination();
        } else {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align: center;">暂无可借工具</td></tr>';
            document.getElementById('paginationContainer').innerHTML = '';
        }
    })
    .catch(err => {
        console.error('加载工具列表失败:', err);
        document.getElementById('toolTableBody').innerHTML =
            '<tr><td colspan="5" style="text-align: center;">加载失败</td></tr>';
    });
}

function renderPagination() {
    const container = document.getElementById('paginationContainer');
    const pageSize = 10;
    const totalPages = Math.ceil(toolTotal / pageSize);
    container.innerHTML = `
        <button class="btn btn-default btn-sm" onclick="loadAvailableTools(${toolPageNum - 1})" ${toolPageNum <= 1 ? 'disabled' : ''}>上一页</button>
        <span style="margin: 0 5px;">第</span>
        <input type="number" id="toolPageInput" value="${toolPageNum}" min="1" max="${totalPages}" style="width: 50px; text-align: center; padding: 2px;" onkeypress="if(event.key==='Enter'){let p=parseInt(this.value);if(p>=1&&p<=${totalPages})loadAvailableTools(p);}">
        <span style="margin: 0 5px;">/ ${totalPages} 页，共 ${toolTotal} 条</span>
        <button class="btn btn-default btn-sm" onclick="loadAvailableTools(${toolPageNum + 1})" ${toolPageNum >= totalPages ? 'disabled' : ''}>下一页</button>
    `;
}

function selectTool(toolCode) {
    selectedToolCode = toolCode;
    document.getElementById('borrowBtn').disabled = false;
    document.getElementById('tips').innerHTML = '已选择工具 ' + toolCode + '，确认后柜门将开启，请在3分钟内取出工具！';
}

function searchTools() {
    const keyword = document.getElementById('searchKeyword').value;
    const toolType = document.getElementById('searchToolType').value;
    toolPageNum = 1;
    loadAvailableTools(1, keyword, toolType);
}

function confirmBorrow() {
    if (!selectedToolCode) {
        alert('请选择要借的工具');
        return;
    }

    if (!confirm('确认借取工具 ' + selectedToolCode + '？')) {
        return;
    }

    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    fetch(API_BASE_URL + '/employee/tool/borrow?toolCode=' + encodeURIComponent(selectedToolCode), {
        method: 'POST',
        headers: {
            [AUTH_HEADER]: token,
            'Content-Type': 'application/json'
        }
    })
    .then(res => res.json())
    .then(data => {
        if (data.code === 200) {
            alert('借工具成功！');
            selectedToolCode = null;
            document.getElementById('borrowBtn').disabled = true;
            document.getElementById('tips').innerHTML = '借工具成功！请在3分钟内取出工具。';
            document.querySelectorAll('input[name="tool"]').forEach(r => r.checked = false);
            loadAvailableTools();
        } else {
            alert('借工具失败：' + (data.message || '未知错误'));
        }
    })
    .catch(err => {
        console.error('借工具失败:', err);
        alert('借工具失败，请稍后重试');
    });
}

document.addEventListener('DOMContentLoaded', function() {
    loadAvailableTools();

    const searchInput = document.getElementById('searchKeyword');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchTools();
            }
        });
    }
});