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
adb("shell", "am", "start", "-W", "-n", pkg + "/.Home")
tree = screen()
button = tree.find(".//node[@text='Dinlemeyi aç']")
x, y = center(button)
adb("shell", "input", "tap", str(x), str(y))
# Exercise actual first-use permission dialogs without pressing Home.
for attempt in range(12):
    tree = screen()
    for suffix in ("permission_allow_foreground_only_button", "permission_allow_button"):
        choice = tree.find(".//node[@resource-id='com.android.permissioncontroller:id/" + suffix + "']")
        if choice is None:
            choice = next((n for n in tree.iter("node") if n.get("resource-id", "").endswith(":id/" + suffix)), None)
        if choice is not None:
            x, y = center(choice)
            adb("shell", "input", "tap", str(x), str(y))
            break
    else:
        service = adb("shell", "dumpsys", "activity", "services", pkg)
        if "isForeground=true" in service and tree.find(".//node[@package='" + pkg + "']") is None:
            break
else:
    raise RuntimeError("Permission flow failed to start listening and leave the foreground")

# Reopening an already active listener must keep its controls accessible.
adb("shell", "am", "start", "-W", "-n", pkg + "/.Home")
assert screen().find(".//node[@text='Dinleme açık']") is not None
adb("shell", "input", "keyevent", "KEYCODE_SLEEP")
time.sleep(5)
service = adb("shell", "dumpsys", "activity", "services", pkg)
assert ".Listener" in service and "isForeground=true" in service, service
adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
