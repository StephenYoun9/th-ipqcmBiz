function loadDashboardStats() {
  fetch(API_BASE_URL + '/dashboard/stats', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'}
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

function loadAlerts() {
  fetch(API_BASE_URL + '/dashboard/alerts', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'}
  })
  .then(res => res.json())
  .then(data => {
    const container = document.getElementById('alertsContainer');
    if (data.code === 200 && data.data && data.data.length > 0) {
      let html = '';
      data.data.forEach(alert => {
        const typeClass = alert.exceptionType === '紧急' ? 'alert-danger' : 'alert-warning';
        const typeLabel = alert.exceptionType === '紧急' ? '紧急告警' : '提醒';
        const desc = alert.description || alert.exceptionType || '有异常待处理';
        html += `<div class="alert ${typeClass}">
          <strong>${typeLabel}：</strong>${desc}
        </div>`;
      });
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

document.addEventListener('DOMContentLoaded', function() {
  loadDashboardStats();
  loadAlerts();
});