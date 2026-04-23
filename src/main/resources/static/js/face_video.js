// 引入flv.js（需提前下载或CDN引入：https://cdn.jsdelivr.net/npm/flv.js@1.6.2/dist/flv.min.js）
let flvPlayer = null;
const statusBox = document.getElementById("statusBox");
const videoFrame = document.getElementById("videoFrame");

// 更新状态
function updateStatus(msg) {
    const time = new Date().toLocaleTimeString();
    statusBox.innerText = `[${time}] ${msg}`;
}

// 启动预览（直接播放FLV流）
function startPreview() {
    if (flvPlayer) {
        updateStatus("预览已在运行中");
        return;
    }
    // 检查flv.js兼容性
    if (!flvjs.isSupported()) {
        updateStatus(" 当前浏览器不支持FLV播放");
        alert("当前浏览器不支持FLV播放，请使用Chrome/Firefox");
        return;
    }

    updateStatus("正在连接摄像头流媒体...");
    // 先请求后端启动转码服务
    fetch("/face-video/reinit")
        .then(res => res.json())
        .then(res => {
            if (res.code !== 200) throw new Error(res.msg);
            // 初始化FLV播放器
            flvPlayer = flvjs.createPlayer({
                type: 'flv',
                url: res.data, // 后端返回的FLV流地址（如http://localhost:3030/stream.flv）
                isLive: true,  // 实时流模式
                hasAudio: false, // 摄像头无音频则关闭
                enableStashBuffer: false // 禁用缓冲（零延迟关键）
            }, {
                enableWorker: true,
                lazyLoadMaxDuration: 0,
                seekType: 'range'
            });

            // 绑定视频元素
            flvPlayer.attachMediaElement(videoFrame);
            // 监听播放事件
            flvPlayer.on('ready', () => {
                updateStatus(" 流媒体连接成功，开始实时播放");
                flvPlayer.play();
            });
            flvPlayer.on('error', (err) => {
                updateStatus(" 播放异常：" + err.message);
                stopPreview();
            });
            flvPlayer.load();
        })
        .catch(err => {
            updateStatus(" 启动失败：" + err.message);
            alert("启动失败：" + err.message);
        });
}

// 停止预览
function stopPreview() {
    if (flvPlayer) {
        flvPlayer.pause();
        flvPlayer.unload();
        flvPlayer.detachMediaElement();
        flvPlayer.destroy();
        flvPlayer = null;
    }
    videoFrame.src = "";
    fetch("/face-video/release").catch(() => {});
    updateStatus("已停止预览");
}

// 页面关闭释放资源
window.onbeforeunload = stopPreview;