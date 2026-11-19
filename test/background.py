"""Check the signed app's layout and foreground microphone service on a device."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True)


def screen():
    adb("shell", "uiautomator", "dump", "/sdcard/alkis-window.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/alkis-window.xml"))


def center(node):
    left, top, right, bottom = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    return (left + right) // 2, (top + bottom) // 2


pkg = "tr.olgun.alkis"
for permission in ("RECORD_AUDIO", "POST_NOTIFICATIONS"):
    adb("shell", "pm", "grant", pkg, "android.permission." + permission)
adb("shell", "am", "start", "-W", "-n", pkg + "/.Home")
tree = screen()
button = tree.find(".//node[@text='Dinlemeyi aç']")
x, y = center(button)
adb("shell", "input", "tap", str(x), str(y))
for attempt in range(8):
    if screen().find(".//node[@text='Dinleme açık']") is not None:
        break
else:
    raise RuntimeError("Microphone listener did not start")
adb("shell", "input", "keyevent", "KEYCODE_HOME")
adb("shell", "input", "keyevent", "KEYCODE_SLEEP")
time.sleep(5)
service = adb("shell", "dumpsys", "activity", "services", pkg)
assert ".Listener" in service and "isForeground=true" in service, service
adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
