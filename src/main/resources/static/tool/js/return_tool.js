let selectedReturnToolCode = null;
let returnPageNum = 1;
let returnTotal = 0;

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes;
}

function loadMyBorrowedTools(pageNum) {
    if (pageNum) returnPageNum = pageNum;
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    const pageSize = 10;

    fetch(API_BASE_URL + '/employee/tool/my-borrowed?pageNum=' + returnPageNum + '&pageSize=' + pageSize, {
        method: 'GET',
        headers: {
            [AUTH_HEADER]: token,
            'Content-Type': 'application/json'
        }
    })
    .then(res => res.json())
    .then(data => {
        const tbody = document.getElementById('borrowedTableBody');
        if (data.code === 200 && data.data && data.data.list && data.data.list.length > 0) {
            returnTotal = data.data.total;
            let html = '';
            data.data.list.forEach(record => {
                html += `<tr>
                    <td><input type="radio" name="returnTool" value="${record.toolCode}" onchange="selectReturnTool('${record.toolCode}')"></td>
                    <td>${record.toolCode}</td>
                    <td>${formatDate(record.borrowTime)}</td>
                </tr>`;
            });
            tbody.innerHTML = html;
            renderPagination();
        } else {
            tbody.innerHTML = '<tr><td colspan="3" style="text-align: center;">暂无未归还工具</td></tr>';
            document.getElementById('paginationContainer').innerHTML = '';
        }
    })
    .catch(err => {
        console.error('加载未还工具失败:', err);
        document.getElementById('borrowedTableBody').innerHTML =
            '<tr><td colspan="3" style="text-align: center;">加载失败</td></tr>';
    });
}

function renderPagination() {
    const container = document.getElementById('paginationContainer');
    const pageSize = 10;
    const totalPages = Math.ceil(returnTotal / pageSize);
    container.innerHTML = `
        <button class="btn btn-default btn-sm" onclick="loadMyBorrowedTools(${returnPageNum - 1})" ${returnPageNum <= 1 ? 'disabled' : ''}>上一页</button>
        <span style="margin: 0 5px;">第</span>
        <input type="number" id="returnPageInput" value="${returnPageNum}" min="1" max="${totalPages}" style="width: 50px; text-align: center; padding: 2px;" onkeypress="if(event.key==='Enter'){let p=parseInt(this.value);if(p>=1&&p<=${totalPages})loadMyBorrowedTools(p);}">
        <span style="margin: 0 5px;">/ ${totalPages} 页，共 ${returnTotal} 条</span>
        <button class="btn btn-default btn-sm" onclick="loadMyBorrowedTools(${returnPageNum + 1})" ${returnPageNum >= totalPages ? 'disabled' : ''}>下一页</button>
    `;
}

function selectReturnTool(toolCode) {
    selectedReturnToolCode = toolCode;
    document.getElementById('returnBtn').disabled = false;
    document.getElementById('returnTips').innerHTML = '已选择工具 ' + toolCode + '，确认后归还柜门将开启，请放入对应工具！';
}

function confirmReturn() {
    if (!selectedReturnToolCode) {
        alert('请选择要归还的工具');
        return;
    }

    if (!confirm('确认归还工具 ' + selectedReturnToolCode + '？')) {
        return;
    }

    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    fetch(API_BASE_URL + '/employee/tool/return?toolCode=' + encodeURIComponent(selectedReturnToolCode), {
        method: 'POST',
        headers: {
            [AUTH_HEADER]: token,
            'Content-Type': 'application/json'
        }
    })
    .then(res => res.json())
    .then(data => {
        if (data.code === 200) {
            alert('还工具成功！');
            selectedReturnToolCode = null;
            document.getElementById('returnBtn').disabled = true;
            document.getElementById('returnTips').innerHTML = '还工具成功！请将工具放入对应柜门。';
            document.querySelectorAll('input[name="returnTool"]').forEach(r => r.checked = false);
            loadMyBorrowedTools();
        } else {
            alert('还工具失败：' + (data.message || '未知错误'));
        }
    })
    .catch(err => {
        console.error('还工具失败:', err);
        alert('还工具失败，请稍后重试');
    });
}

document.addEventListener('DOMContentLoaded', function() {
    loadMyBorrowedTools();
});