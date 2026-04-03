let userName = "";
let userId = "";
let collecting = false;
let progress = 0;

// loop方法：失败次数限制+初始化失败提示
let requestFailCount = 0; // 新增：请求失败计数器
const MAX_FAIL_COUNT = 5; // 最大失败次数，超过则停止请求

// 前置检查（调用后端状态接口，判断核心对象是否初始化）
function startCollect() {
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

    // 新增：先检查后端核心对象是否初始化
    updateStatus("检查摄像头状态...");
    fetch("/face/status")
        .then(res => res.json())
        .then(data => {
            // 调用reinit接口触发后端重试初始化
            fetch("/face/reinit")
                .then(res => res.json())
                .then(reinitData => {
                    if (reinitData.code === 200) {
                        collecting = true;
                        updateStatus("正在打开摄像头...");
                        loop();
                    } else {
                        updateStatus("摄像头初始化失败：" + reinitData.message);
                        alert("摄像头初始化失败：" + reinitData.message);
                    }
                });
        })
        .catch(err => {
            updateStatus("检查摄像头状态失败：" + err.message);
            alert("无法连接到后端，请检查服务是否正常！");
        });
}

// 循环请求后端画面
function loop() {
    if (!collecting) return;

    fetch("/face/frame?userName=" + encodeURIComponent(userName) + "&userId=" + encodeURIComponent(userId))
        .then(res => {
            // 先判断响应是否正常
            if (!res.ok) {
                throw new Error(`接口返回异常：${res.status}`);
            }
            return res.blob();
        })
        .then(blob => {
            // 重置失败计数器
            requestFailCount = 0;
            let url = URL.createObjectURL(blob);
            document.getElementById("videoFrame").src = url;

            // 请求状态接口
            fetch("/face/status")
                .then(res => res.json())
                .then(data => {
                    document.getElementById("progress").innerText = data.progress + "/10";
                    document.getElementById("guide").innerText = data.direction;
                    document.getElementById("status").innerText = "状态：" + data.msg;

                    if (data.progress >= 10) {
                        collecting = false;
                        updateStatus("采集完成！");
                    }
                });

            setTimeout(loop, 50);
        })
        .catch(err => {
            requestFailCount++;
            console.error("帧请求失败（次数：" + requestFailCount + "）：", err);
            // 超过最大失败次数，停止采集并提示
            if (requestFailCount >= MAX_FAIL_COUNT) {
                collecting = false;
                updateStatus("采集失败：摄像头初始化异常，请联系管理员检查设备！");
                // 提示用户并提供重试按钮（可选）
                alert("摄像头初始化失败，无法采集人脸！\n1. 检查摄像头是否被其他程序占用\n2. 刷新页面重试\n3. 联系管理员检查设备");
                return;
            }
            // 失败后延迟重试，避免高频请求
            setTimeout(loop, 500);
        });
}

// 更新状态
function updateStatus(msg) {
    document.getElementById("status").innerText = "状态：" + msg;
}

// 页面离开时停止采集
window.onbeforeunload = function () {
    collecting = false;
};
window.onbeforeunload = function () {
    collecting = false;
    if (typeof releaseCamera === 'function') {
        releaseCamera();
    }
};

function releaseCamera() {
    console.log("主动调用后端释放摄像头接口");
    // 替换为你的实际接口地址
    fetch('/face/releaseCamera', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(res => {
        console.log("摄像头释放接口调用成功");
    }).catch(err => {
        console.error("摄像头释放接口调用失败", err);
    });
}