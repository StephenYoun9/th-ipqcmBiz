document.getElementById('borrowBtn').addEventListener('click', function() {
    const selectedTool = document.querySelector('input[name="tool"]:checked');
    if (!selectedTool) {
        document.getElementById('tips').className = 'alert alert-danger';
        document.getElementById('tips').innerText = '请先选择要借取的工具！';
        return;
    }

    document.getElementById('tips').className = 'alert alert-success';
    document.getElementById('tips').innerText = '已选择工具：' + selectedTool.value + '，对应柜门已开启，请取出工具，系统将自动采集影像确认！';
});