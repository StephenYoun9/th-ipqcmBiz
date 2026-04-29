/**
 * 操作日志管理JS
 */

let currentPage = 1;
let pageSize = 10;
let totalPages = 0;

function queryLogs() {
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    const operatorKeyword = document.getElementById('operatorKeyword').value.trim();
    const startTime = document.getElementById('startTime').value;
    const endTime = document.getElementById('endTime').value;
    const operatorType = document.getElementById('operatorType').value;
    const exceptionOnly = document.getElementById('exceptionOnly').checked;

    const params = new URLSearchParams();
    if (operatorKeyword) params.append('operatorKeyword', operatorKeyword);
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    if (operatorType) params.append('operatorType', operatorType);
    if (exceptionOnly) params.append('exceptionOnly', 'true');
    params.append('pageNum', currentPage);
    params.append('pageSize', pageSize);

    fetch(API_BASE_URL + '/admin/log/list?' + params.toString(), {
        headers: { [AUTH_HEADER]: token }
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            renderLogs(data.data);
            updatePagination(data.data);
            updateExceptionAlert(data.data.exceptionCount);
        } else {
            alert(data.message || '查询失败');
        }
    })
    .catch(err => {
        console.error('查询日志失败:', err);
        alert('查询失败，请检查网络');
    });
}

function renderLogs(pageData) {
    const tbody = document.getElementById('logTableBody');
    if (!tbody) return;

    const logs = pageData.list || [];

    if (logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = logs.map(log => {
        const exceptionClass = log.hasException ? 'style="background-color: #f8d7da;"' : '';
        const exceptionTag = log.hasException ?
            '<span style="color: #dc3545; font-weight: bold;">&#9888; 异常</span>' :
            '<span style="color: #28a745;">正常</span>';

        return '<tr ' + exceptionClass + '>' +
            '<td>' + escapeHtml(log.operatorName || '-') + '</td>' +
            '<td>' + formatDate(log.operateTime) + '</td>' +
            '<td>' + escapeHtml(log.operatorTypeName || '-') + '</td>' +
            '<td>' + escapeHtml(log.detail || '-') + '</td>' +
            '<td>' + exceptionTag + '</td>' +
            '</tr>';
    }).join('');
}

function updatePagination(pageData) {
    currentPage = pageData.pageNum;
    totalPages = pageData.totalPages;
    const total = pageData.total;

    const pageInfo = document.getElementById('pageInfo');
    if (pageInfo) {
        pageInfo.textContent = '第' + currentPage + '页 / 共' + totalPages + '页，共' + total + '条';
    }

    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    if (prevBtn) prevBtn.disabled = currentPage <= 1;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages;
}

function updateExceptionAlert(exceptionCount) {
    const alertBox = document.getElementById('exceptionAlert');
    if (alertBox) {
        if (exceptionCount && exceptionCount > 0) {
            alertBox.style.display = 'block';
            alertBox.innerHTML = '发现' + exceptionCount + '条异常日志，请及时核查！';
        } else {
            alertBox.style.display = 'none';
        }
    }
}

function prevPage() {
    if (currentPage > 1) {
        currentPage--;
        queryLogs();
    }
}

function nextPage() {
    if (currentPage < totalPages) {
        currentPage++;
        queryLogs();
    }
}

function exportLogs() {
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
    const operatorKeyword = document.getElementById('operatorKeyword').value.trim();
    const startTime = document.getElementById('startTime').value;
    const endTime = document.getElementById('endTime').value;
    const operatorType = document.getElementById('operatorType').value;
    const exceptionOnly = document.getElementById('exceptionOnly').checked;

    const params = new URLSearchParams();
    if (operatorKeyword) params.append('operatorKeyword', operatorKeyword);
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    if (operatorType) params.append('operatorType', operatorType);
    if (exceptionOnly) params.append('exceptionOnly', 'true');

    window.open(API_BASE_URL + '/admin/log/export?' + params.toString(), '_blank');
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');

    return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', function() {
    queryLogs();
});