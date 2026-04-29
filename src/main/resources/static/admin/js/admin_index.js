let currentEditAnnouncementId = null;
let selectedAttachments = [];
let uploadedAttachmentUrls = [];

function loadDashboardStats() {
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  fetch(API_BASE_URL + '/dashboard/stats', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [AUTH_HEADER]: token
    }
  })
  .then(res => res.json())
  .then(data => {
    if (data.code === 200 && data.data) {
      document.getElementById('employeeCount').textContent = data.data.employeeCount || 0;
      document.getElementById('toolCount').textContent = data.data.toolCount || 0;
      document.getElementById('todayBorrowCount').textContent = data.data.todayBorrowCount || 0;
      document.getElementById('pendingExceptionCount').textContent = data.data.pendingExceptionCount || 0;
    }
  })
  .catch(err => console.error('Failed to load dashboard stats:', err));
}

let alertsPageNum = 1;
let alertsTotal = 0;

function loadAlerts(pageNum) {
  if (pageNum) alertsPageNum = pageNum;
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  const pageSize = 5;
  fetch(API_BASE_URL + '/dashboard/alerts?pageNum=' + alertsPageNum + '&pageSize=' + pageSize, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [AUTH_HEADER]: token
    }
  })
  .then(res => res.json())
  .then(data => {
    const container = document.getElementById('alertsContainer');
    if (data.code === 200 && data.data && data.data.list && data.data.list.length > 0) {
      alertsTotal = data.data.total;
      let html = '';
      data.data.list.forEach(alert => {
        const typeClass = alert.exceptionType === '紧急' ? 'alert-danger' : 'alert-warning';
        const typeLabel = alert.exceptionType === '紧急' ? '紧急告警' : '提醒';
        const desc = alert.description || alert.exceptionType || '有异常待处理';
        html += `<div class="alert ${typeClass}">
          <strong>${typeLabel}：</strong>${escapeHtml(desc)}
        </div>`;
      });

      const totalPages = Math.ceil(alertsTotal / pageSize);
      html += `<div style="margin-top: 15px; text-align: center;">
        <button class="btn btn-default btn-sm" onclick="loadAlerts(${alertsPageNum - 1})" ${alertsPageNum <= 1 ? 'disabled' : ''}>上一页</button>
        <span style="margin: 0 5px;">第</span>
        <input type="number" id="alertsPageInput" value="${alertsPageNum}" min="1" max="${totalPages}" style="width: 50px; text-align: center; padding: 2px;" onkeypress="if(event.key==='Enter'){let p=parseInt(this.value);if(p>=1&&p<=${totalPages})loadAlerts(p);}">
        <span style="margin: 0 5px;">/ ${totalPages} 页，共 ${alertsTotal} 条</span>
        <button class="btn btn-default btn-sm" onclick="loadAlerts(${alertsPageNum + 1})" ${alertsPageNum >= totalPages ? 'disabled' : ''}>下一页</button>
      </div>`;

      container.innerHTML = html;
    } else {
      container.innerHTML = '<div class="alert alert-success">暂无异常告警</div>';
    }
  })
  .catch(err => {
    console.error('Failed to load alerts:', err);
    document.getElementById('alertsContainer').innerHTML =
      '<div class="alert alert-warning">加载告警失败</div>';
  });
}

let announcementPageNum = 1;
let announcementTotal = 0;

function loadAnnouncements(pageNum) {
  if (pageNum) announcementPageNum = pageNum;
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  const pageSize = 5;
  fetch(API_BASE_URL + '/admin/announcement/list?pageNum=' + announcementPageNum + '&pageSize=' + pageSize, {
    headers: { [AUTH_HEADER]: token }
  })
  .then(res => res.json())
  .then(data => {
    const container = document.getElementById('announcementList');
    if (data.code === 200 && data.data && data.data.list && data.data.list.length > 0) {
      announcementTotal = data.data.total;
      let html = '';
      data.data.list.forEach(item => {
        const typeClass = item.type === 'danger' ? 'alert-danger' :
                          item.type === 'warning' ? 'alert-warning' : 'alert-info';
        const isExpired = item.expireTime && new Date(item.expireTime) < new Date();
        const expiredStyle = isExpired ? 'opacity: 0.6; background-color: #f5f5f5;' : '';
        let attachmentsHtml = '';
        if (item.attachments && item.attachments.length > 0) {
          attachmentsHtml = '<div style="margin-top: 10px; display: flex; flex-wrap: wrap; gap: 5px;">';
          item.attachments.forEach(url => {
            if (url && url.match(/\.(jpg|jpeg|png|gif)$/i)) {
              attachmentsHtml += `<img src="${API_BASE_URL}${url}" style="max-width: 100px; max-height: 80px; border-radius: 4px; cursor: pointer;" onclick="openImagePreview('${API_BASE_URL}${url}')">`;
            } else if (url && url.match(/\.(mp4|avi|mov|wmv)$/i)) {
              attachmentsHtml += `<video src="${API_BASE_URL}${url}" style="max-width: 100px; max-height: 80px;"></video>`;
            }
          });
          attachmentsHtml += '</div>';
        }
        html += `<div class="alert ${typeClass}" style="margin-bottom: 10px; ${expiredStyle}">
          <strong>${escapeHtml(item.title)}</strong>${isExpired ? ' <span style="background: #999; color: white; padding: 2px 8px; border-radius: 4px; font-size: 12px;">已失效</span>' : ''}
          <p style="margin: 5px 0;">${escapeHtml(item.content)}</p>
          ${attachmentsHtml}
          <small>发布人：${escapeHtml(item.publisherName)} | ${formatDate(item.createTime)}</small>
          ${item.expireTime ? ' | <small>失效时间：' + formatDate(item.expireTime) + '</small>' : ''}
          <div style="float: right; margin-top: -5px;">
            <button class="btn btn-primary btn-sm" style="margin-right: 5px;" onclick="editAnnouncement(${item.id}, '${escapeHtml(item.title)}', '${escapeHtml(item.content)}', '${item.type}', '${item.attachments ? item.attachments.join(',') : ''}', '${item.expireTime || ''}')">编辑</button>
            <button class="btn btn-danger btn-sm" onclick="deleteAnnouncement(${item.id})">删除</button>
          </div>
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
      container.innerHTML = '<div class="alert alert-info">暂无公告</div>';
    }
  })
  .catch(err => {
    console.error('Failed to load announcements:', err);
    document.getElementById('announcementList').innerHTML =
      '<div class="alert alert-warning">加载公告失败</div>';
  });
}

function openAnnouncementModal(id, title, content, type, attachments, expireTime) {
  currentEditAnnouncementId = id;
  document.getElementById('announcementTitle').value = title || '';
  document.getElementById('announcementContent').value = content || '';
  document.getElementById('announcementType').value = type || 'info';

  if (expireTime) {
    const date = new Date(expireTime);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    document.getElementById('announcementExpireTime').value = `${year}-${month}-${day}T${hours}:${minutes}`;
  } else {
    document.getElementById('announcementExpireTime').value = '';
  }

  uploadedAttachmentUrls = [];
  selectedAttachments = [];
  if (attachments) {
    uploadedAttachmentUrls = attachments.split(',').filter(u => u);
  }
  updateAttachmentPreview();
  updateCharCount();

  if (id) {
    document.getElementById('announcementModalTitle').textContent = '编辑公告';
    document.getElementById('publishBtn').textContent = '保存修改';
  } else {
    document.getElementById('announcementModalTitle').textContent = '发布系统公告';
    document.getElementById('publishBtn').textContent = '发布';
  }

  document.getElementById('announcementModal').style.display = 'flex';
}

function editAnnouncement(id, title, content, type, attachments, expireTime) {
  openAnnouncementModal(id, title, content, type, attachments, expireTime);
}

function closeAnnouncementModal() {
  currentEditAnnouncementId = null;
  selectedAttachments = [];
  uploadedAttachmentUrls = [];
  document.getElementById('announcementAttachments').value = '';
  document.getElementById('announcementModal').style.display = 'none';
}

function updateCharCount() {
  const title = document.getElementById('announcementTitle').value;
  const content = document.getElementById('announcementContent').value;
  document.getElementById('titleCount').textContent = title.length + '/256';
  document.getElementById('contentCount').textContent = content.length + '/2048';
}

function handleAttachmentSelect(input) {
  if (input.files && input.files.length > 0) {
    const files = Array.from(input.files);
    files.forEach(file => {
      selectedAttachments.push(file);
    });
    updateAttachmentPreview();
  }
  input.value = '';
}

function updateAttachmentPreview() {
  const container = document.getElementById('attachmentPreview');
  const info = document.getElementById('attachmentInfo');

  if (uploadedAttachmentUrls.length === 0 && selectedAttachments.length === 0) {
    container.innerHTML = '';
    info.textContent = '';
    return;
  }

  let html = '';

  uploadedAttachmentUrls.forEach((url, index) => {
    if (url.match(/\.(jpg|jpeg|png|gif)$/i)) {
      html += `<div style="position: relative; width: 80px; height: 80px;">
        <img src="${API_BASE_URL}${url}" style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px; border: 1px solid #ddd;">
        <button onclick="removeUploadedAttachment(${index})" style="position: absolute; top: -5px; right: -5px; width: 20px; height: 20px; border-radius: 50%; background: red; color: white; border: none; cursor: pointer; font-size: 12px;">×</button>
      </div>`;
    } else if (url.match(/\.(mp4|avi|mov|wmv)$/i)) {
      html += `<div style="position: relative; width: 80px; height: 80px;">
        <video src="${API_BASE_URL}${url}" style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px; border: 1px solid #ddd;"></video>
        <button onclick="removeUploadedAttachment(${index})" style="position: absolute; top: -5px; right: -5px; width: 20px; height: 20px; border-radius: 50%; background: red; color: white; border: none; cursor: pointer; font-size: 12px;">×</button>
      </div>`;
    }
  });

  selectedAttachments.forEach((file, index) => {
    const url = URL.createObjectURL(file);
    if (file.type.startsWith('image/')) {
      html += `<div style="position: relative; width: 80px; height: 80px;">
        <img src="${url}" style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px; border: 1px solid #ddd; opacity: 0.7;">
        <button onclick="removeSelectedAttachment(${index})" style="position: absolute; top: -5px; right: -5px; width: 20px; height: 20px; border-radius: 50%; background: red; color: white; border: none; cursor: pointer; font-size: 12px;">×</button>
      </div>`;
    } else if (file.type.startsWith('video/')) {
      html += `<div style="position: relative; width: 80px; height: 80px;">
        <video src="${url}" style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px; border: 1px solid #ddd; opacity: 0.7;"></video>
        <button onclick="removeSelectedAttachment(${index})" style="position: absolute; top: -5px; right: -5px; width: 20px; height: 20px; border-radius: 50%; background: red; color: white; border: none; cursor: pointer; font-size: 12px;">×</button>
      </div>`;
    }
  });

  container.innerHTML = html;
  info.textContent = `已选择 ${selectedAttachments.length} 个文件`;

  selectedAttachments.forEach((file, index) => {
    const url = URL.createObjectURL(file);
    selectedAttachments[index].previewUrl = url;
  });
}

function removeUploadedAttachment(index) {
  uploadedAttachmentUrls.splice(index, 1);
  updateAttachmentPreview();
}

function removeSelectedAttachment(index) {
  if (selectedAttachments[index] && selectedAttachments[index].previewUrl) {
    URL.revokeObjectURL(selectedAttachments[index].previewUrl);
  }
  selectedAttachments.splice(index, 1);
  updateAttachmentPreview();
}

async function uploadSelectedAttachments() {
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  const uploadedUrls = [];

  for (const file of selectedAttachments) {
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch(API_BASE_URL + '/admin/announcement/attachment', {
        method: 'POST',
        headers: { [AUTH_HEADER]: token },
        body: formData
      });
      const result = await response.json();
      if (result.code == 200) {
        uploadedUrls.push(result.data || result.message);
      }
    } catch (err) {
      console.error('上传附件失败:', err);
    }
  }

  selectedAttachments.forEach(file => {
    if (file.previewUrl) {
      URL.revokeObjectURL(file.previewUrl);
    }
  });
  selectedAttachments = [];

  return uploadedUrls;
}

function publishAnnouncement() {
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  const title = document.getElementById('announcementTitle').value.trim();
  const content = document.getElementById('announcementContent').value.trim();
  const type = document.getElementById('announcementType').value;
  const expireTime = document.getElementById('announcementExpireTime').value;

  if (!title) {
    alert('请填写标题');
    return;
  }

  if (title.length > 256) {
    alert('标题不能超过256字符');
    return;
  }

  if (!content) {
    alert('请填写内容');
    return;
  }

  if (content.length > 2048) {
    alert('内容不能超过2048字符');
    return;
  }

  const publishBtn = document.getElementById('publishBtn');
  publishBtn.disabled = true;
  publishBtn.textContent = '上传中...';

  const doPublish = async (attachmentUrls) => {
    const allAttachments = [...uploadedAttachmentUrls, ...attachmentUrls];

    const data = {
      title: title,
      content: content,
      type: type,
      attachments: allAttachments.length > 0 ? allAttachments : []
    };

    if (expireTime) {
      data.expireTime = expireTime.replace('T', ' ') + ':00';
    }

    let url = API_BASE_URL + '/admin/announcement';
    let method = 'POST';

    if (currentEditAnnouncementId) {
      data.id = currentEditAnnouncementId;
      url = API_BASE_URL + '/admin/announcement/' + currentEditAnnouncementId;
      method = 'PUT';
    }

    try {
      const response = await fetch(url, {
        method: method,
        headers: {
          [AUTH_HEADER]: token,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
      });
      const result = await response.json();
      if (result.code === 200) {
        alert(currentEditAnnouncementId ? '修改成功' : '发布成功');
        closeAnnouncementModal();
        loadAnnouncements();
      } else {
        alert(result.message || '操作失败');
      }
    } catch (err) {
      alert('操作失败');
      console.error(err);
    } finally {
      publishBtn.disabled = false;
      publishBtn.textContent = currentEditAnnouncementId ? '保存修改' : '发布';
    }
  };

  if (selectedAttachments.length > 0) {
    uploadSelectedAttachments().then(uploadedUrls => {
      doPublish(uploadedUrls);
    });
  } else {
    doPublish([]);
  }
}

function deleteAnnouncement(id) {
  if (!confirm('确定要删除这条公告吗？')) return;

  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  fetch(API_BASE_URL + '/admin/announcement/' + id, {
    method: 'DELETE',
    headers: { [AUTH_HEADER]: token }
  })
  .then(res => res.json())
  .then(data => {
    if (data.code === 200) {
      alert('删除成功');
      loadAnnouncements();
    } else {
      alert(data.message || '删除失败');
    }
  })
  .catch(err => {
    alert('删除失败');
    console.error(err);
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
  loadDashboardStats();
  loadAnnouncements();
  loadAlerts();

  document.getElementById('announcementTitle').addEventListener('input', updateCharCount);
  document.getElementById('announcementContent').addEventListener('input', updateCharCount);
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