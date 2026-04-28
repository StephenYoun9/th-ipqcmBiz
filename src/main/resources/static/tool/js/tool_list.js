const filterElements = [
    document.getElementById('toolTypeFilter'),
    document.getElementById('cabinetFilter'),
    document.getElementById('statusFilter')
];

filterElements.forEach(filter => {
    filter.addEventListener('change', function() {
        alert('已选择筛选条件：' + this.options[this.selectedIndex].text + '，点击查询按钮生效！');
    });
});

const borrowButtons = document.querySelectorAll('.tool-actions .btn-primary');
borrowButtons.forEach(btn => {
    btn.addEventListener('click', function() {
        const toolCard = this.closest('.tool-card');
        const toolName = toolCard.querySelector('.tool-name').innerText;
        const toolId = toolCard.querySelector('.tool-detail span:last-child').innerText;

        if (confirm('确认借取【' + toolName + '（' + toolId + '）】吗？')) {
            alert('借取申请已提交，对应柜门将开启，请取出工具！');
        }
    });
});