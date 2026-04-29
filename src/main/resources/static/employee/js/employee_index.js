/**
 * 员工首页JS
 */

let borrowPageNum = 1;
let borrowTotal = 0;
let announcementPageNum = 1;
let announcementTotal = 0;

function loadBorrowRecords(pageNum) {
    if (pageNum) borrowPageNum = pageNum;
    const pageSize = 5;
    const token = sessionStorage.getItem(AUTH_TOKEN_KEY);

    fetch(API_BASE_URL + '/employee/my-borrow-records?pageNum=' + borrowPageNum + '&pageSize=' + pageSize, {
        method: 'POST',
        headers: {
            [AUTH_HEADER]: token,
            'Content-Type': 'application/json'
        }
    })
    .then(res => res.json())
    .then(data => {
        const tbody = document.getElementById('borrowTableBody');
        if (data.code === 200 && data.data && data.data.list && data.data.list.length > 0) {
            borrowTotal = data.data.total;
            let html = '';
            data.data.list.forEach(record => {
                const statusMap = {
                    'BORROWED': '已借出',
                    'RETURNED': '已归还',
                    'OVERDUE': '已逾期'
                };
                const statusText = record.statusName || statusMap[record.status] || record.status;
                const statusClass = record.status === 'BORROWED' ? 'status-warning' : 'status-success';
                html += `<tr>
                    <td>${record.toolCode || '-'}</td>
                    <td>${record.toolName || '-'}</td>
                    <td>${record.cabinetNo || '-'}</td>
                    <td>${formatDate(record.borrowTime)}</td>
                    <td>${formatDate(record.returnTime)}</td>
                    <td><span class="${statusClass}">${statusText}</span></td>
                </tr>`;
            });
            tbody.innerHTML = html;
            renderBorrowPagination();
        } else {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">暂无借还记录</td></tr>';
            document.getElementById('borrowPagination').innerHTML = '';
        }
    })
    .catch(err => {
        console.error('Failed to load borrow records:', err);
        document.getElementById('borrowTableBody').innerHTML =
            '<tr><td colspan="3" style="text-align: center;">加载失败</td></tr>';
    });
}

function renderBorrowPagination() {
    const container = document.getElementById('borrowPagination');
    const totalPages = Math.ceil(borrowTotal / 5);
    container.innerHTML = `
        <button class="btn btn-default btn-sm" onclick="loadBorrowRecords(${borrowPageNum - 1})" ${borrowPageNum <= 1 ? 'disabled' : ''}>上一页</button>
        <span style="margin: 0 5px;">第</span>
        <input type="number" id="borrowPageInput" value="${borrowPageNum}" min="1" max="${totalPages}" style="width: 50px; text-align: center; padding: 2px;" onkeypress="if(event.key==='Enter'){let p=parseInt(this.value);if(p>=1&&p<=${totalPages})loadBorrowRecords(p);}">
        <span style="margin: 0 5px;">/ ${totalPages} 页，共 ${borrowTotal} 条</span>
        <button class="btn btn-default btn-sm" onclick="loadBorrowRecords(${borrowPageNum + 1})" ${borrowPageNum >= totalPages ? 'disabled' : ''}>下一页</button>
    `;
}

function loadAnnouncements(pageNum) {
    if (pageNum) announcementPageNum = pageNum;
    const pageSize = 5;
    fetch(API_BASE_URL + '/common/announcement/paged?pageNum=' + announcementPageNum + '&pageSize=' + pageSize)
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById('announcementContainer');
            if (data.code === 200 && data.data && data.data.list && data.data.list.length > 0) {
                announcementTotal = data.data.total;
                let html = '';
                data.data.list.forEach(item => {
                    const typeClass = item.type === 'danger' ? 'alert-danger' :
                                      item.type === 'warning' ? 'alert-warning' : 'alert-info';
                    let attachmentsHtml = '';
                    if (item.attachments && item.attachments.length > 0) {
                        attachmentsHtml = '<div style="margin-top: 10px; display: flex; flex-wrap: wrap; gap: 10px;">';
                        item.attachments.forEach(url => {
                            if (url && url.match(/\.(jpg|jpeg|png|gif)$/i)) {
                                attachmentsHtml += `<img src="${API_BASE_URL}${url}" style="max-width: 200px; max-height: 150px; border-radius: 4px; border: 1px solid #ddd; cursor: pointer;" onclick="openImagePreview('${API_BASE_URL}${url}')">`;
                            } else if (url && url.match(/\.(mp4|avi|mov|wmv)$/i)) {
                                attachmentsHtml += `<video src="${API_BASE_URL}${url}" controls style="max-width: 300px; max-height: 200px; border-radius: 4px; border: 1px solid #ddd;"></video>`;
                            }
                        });
                        attachmentsHtml += '</div>';
                    }
                    html += `<div class="alert ${typeClass}" style="margin-bottom: 10px;">
                        <strong>${escapeHtml(item.title)}</strong>
                        <p style="margin: 5px 0;">${escapeHtml(item.content)}</p>
                        ${attachmentsHtml}
                        <small>发布于 ${formatDate(item.createTime)}</small>
                    </div>`;
                });

                const totalPages = Math.ceil(announcementTotal / pageSize);
                html += `<div style="margin-top: 15px; text-align: center;">
                    <button class="btn btn-default btn-sm" onclick="loadAnnouncements(${announcementPageNum - 1})" ${announcementPageNum <= 1 ? 'disabled' : ''}>上一页</button>
                    <span style="margin: 0 5px;">第</span>
                    <input type="number" id="announcementPageInput" value="${announcementPageNum}" min="1" max="${totalPages}" style="width: 50px; text-align: center; padding: 2px;" onkeypress="if(event.key==='Enter'){let p=parseInt(this.value);if(p>=1&&p<=${totalPages})loadAnnouncements(p);}">
                    <span style="margin: 0 5px;">/ ${totalPages} 页，共 ${announcementTotal} 条</span>
                    <button class="btn btn-default btn-sm" onclick="loadAnnouncements(${announcementPageNum + 1})" ${announcementPageNum >= totalPages ? 'disabled' : ''}>下一页</button>
                </div>`;

                container.innerHTML = html;
            } else {
                container.innerHTML = '<div class="alert alert-success">暂无公告</div>';
            }
        })
        .catch(err => {
            console.error('Failed to load announcements:', err);
            document.getElementById('announcementContainer').innerHTML =
                '<div class="alert alert-warning">加载公告失败</div>';
        });
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
    return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', function() {
    loadBorrowRecords();
    loadAnnouncements();
});

function openImagePreview(url) {
    const modal = document.getElementById('imagePreviewModal');
    const img = document.getElementById('previewImage');
    img.src = url;
    modal.style.display = 'flex';
}

function closeImagePreview() {
    document.getElementById('imagePreviewModal').style.display = 'none';
}