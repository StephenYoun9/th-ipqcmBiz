document.getElementById('returnBtn').addEventListener('click', function() {
    document.getElementById('returnTips').className = 'alert alert-success';
    document.getElementById('returnTips').innerText = '归还柜门已开启（A01柜），请放入扳手（W001），系统将自动采集影像确认！放入后请关闭柜门，等待识别结果...';

    setTimeout(() => {
        document.getElementById('returnTips').innerText = '影像识别匹配成功！扳手（W001）已归还，工具状态更新为"可借"，归还完成！';
    }, 3000);
});