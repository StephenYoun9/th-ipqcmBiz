let userName = "";
let userId = "";
let collecting = false;
let requestFailCount = 0;
const MAX_FAIL = 10;
let lastBlobUrl = null;
let isReiniting = false;
// 【优化】请求间隔改为100ms（每秒10帧，人眼无感知卡顿，同时降低服务器压力）
const FRAME_INTERVAL = 100;
// 防抖定时器
let loopTimer = null;

function startCollect() {
    // 强制重置所有状态
    collecting = false;
    requestFailCount = 0;
    isReiniting = false;
    if (loopTimer) clearTimeout(loopTimer);
    if (lastBlobUrl) {
        URL.revokeObjectURL(lastBlobUrl);
        lastBlobUrl = null;
    }

    userName = document.getElementById("userName").value.trim();
    userId = document.getElementById("userId").value.trim();

    if (!userName) {
        alert("请输入姓名");
        return;
    }
    if (!userId) {
        alert("请输入用户编号");
        return;
    }

    if (isReiniting) {
        alert("正在初始化摄像头，请稍候...");
        return;
    }
    isReiniting = true;
    updateStatus("正在初始化摄像头...");

    fetch("/face/releaseCamera")
        .then(() => fetch("/face/reinit"))
        .then(res => {
            if (!res.ok) throw new Error(`HTTP错误：${res.status} ${res.statusText}`);
            return res.json();
        })
        .then(data => {
            if (data.code !== 200) {
                alert("摄像头初始化失败：" + data.msg);
                isReiniting = false;
                return;
            }
            collecting = true;
            isReiniting = false;
            updateStatus("正在采集...");
            loop();
        })
        .catch(err => {
            console.error("后端服务异常详情：", err);
            alert("服务异常，请检查后端服务：" + err.message);
            isReiniting = false;
        });
}

/**
 * 【优化】帧循环：避免重复请求，保证画面流畅
 */
function loop() {
    if (!collecting) return;

    // 防抖：避免重复发起请求
    if (loopTimer) clearTimeout(loopTimer);

    fetch("/face/frame?userName=" + encodeURIComponent(userName) + "&userId=" + encodeURIComponent(userId))
        .then(res => {
            if (!res.ok) throw new Error("帧请求失败");
            return res.blob();
        })
        .then(blob => {
            // 先释放旧blob，再创建新的，彻底解决内存泄漏
            if (lastBlobUrl) {
                URL.revokeObjectURL(lastBlobUrl);
            }
            lastBlobUrl = URL.createObjectURL(blob);
            // 【优化】只在src变化时更新，避免重复渲染
            const videoFrame = document.getElementById("videoFrame");
            if (videoFrame.src !== lastBlobUrl) {
                videoFrame.src = lastBlobUrl;
            }
            requestFailCount = 0;

            // 异步更新状态，不阻塞画面
            fetch("/face/status")
                .then(res => res.json())
                .then(data => {
                    document.getElementById("progress").innerText = data.data.progress + "/10";
                    document.getElementById("guide").innerText = data.data.direction;
                    document.getElementById("status").innerText = "状态：" + data.data.msg;

                    if (data.data.progress >= 10) {
                        collecting = false;
                        updateStatus("采集完成！");
                    }
                });

            // 下一次循环
            loopTimer = setTimeout(loop, FRAME_INTERVAL);
        })
        .catch(err => {
            requestFailCount++;
            console.error("帧请求失败：", requestFailCount, err);
            if (requestFailCount > MAX_FAIL) {
                collecting = false;
                updateStatus("连接失败，请刷新重试");
                alert("摄像头连接失败，请检查设备或刷新页面重试");
                return;
            }
            // 失败后快速重试
            loopTimer = setTimeout(loop, 50);
        });
}

function updateStatus(msg) {
    document.getElementById("status").innerText = "状态：" + msg;
}

// 页面离开时彻底释放资源
window.onbeforeunload = function () {
    collecting = false;
    isReiniting = false;
    if (loopTimer) clearTimeout(loopTimer);
    if (lastBlobUrl) {
        URL.revokeObjectURL(lastBlobUrl);
    }
    fetch("/face/releaseCamera");
};