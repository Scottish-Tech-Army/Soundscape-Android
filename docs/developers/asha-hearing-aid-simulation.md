---
title: ASHA hearing aid simulation
layout: page
parent: Information for developers
has_toc: true
---

# ASHA hearing aid simulation with Bumble

[ASHA (Audio Streaming for Hearing Aids)](https://source.android.com/docs/core/connect/bluetooth/asha) is the Google protocol used by Android to stream audio to BLE hearing aids. Soundscape uses ASHA to send navigation audio directly to a user's hearing aids. This page explains how to run a simulated hearing aid on a Linux PC using [Google Bumble](https://github.com/google/bumble), so that you can test the ASHA connection path without needing a real hearing aid device.

## Hardware requirements

You need a Bluetooth **4.0 or later** USB dongle with LE support on the Linux machine running the simulation. ASHA requires BLE; BT 1.x/2.x/3.x dongles will not work.

A dongle that has been confirmed to work is the **BT820** (`USB ID 0a12:0001`, reports `BLUETOOTH_CORE_4_0`). Dongles reporting BT 5.0 should also work.

## Setting up Bumble

Clone the Bumble repository and create a virtual environment:

```bash
git clone https://github.com/google/bumble.git
cd bumble
python3 -m venv .venv
.venv/bin/pip install -e ".[development]"
```

### Required patch to `run_asha_sink.py`

The upstream `examples/run_asha_sink.py` uses the extended advertising API (`create_advertising_set`), which requires a BT 5.0 controller and is also not recognised as legacy advertising by Android's ASHA scanner. Apply the following change before running:

In `examples/run_asha_sink.py`, replace the import line:

```python
from bumble.device import AdvertisingEventProperties, AdvertisingParameters, Device
```

with:

```python
from bumble.device import Device
```

And replace the `create_advertising_set` call:

```python
await device.create_advertising_set(
    auto_restart=True,
    advertising_data=advertising_data,
    advertising_parameters=AdvertisingParameters(
        primary_advertising_interval_min=100,
        primary_advertising_interval_max=100,
    ),
)
```

with:

```python
await device.start_advertising(
    auto_restart=True,
    advertising_data=advertising_data,
    advertising_interval_min=100,
    advertising_interval_max=100,
)
```

`start_advertising` automatically falls back to legacy HCI advertising commands when the controller does not support extended advertising, and produces legacy advertising PDUs that Android's hearing aid scanner can discover.

## Running the simulation

Bumble takes direct control of the USB dongle, bypassing the kernel Bluetooth stack, so it needs root (or a udev rule granting access to the device). From the `bumble` directory:

```bash
sudo .venv/bin/python3 examples/run_asha_sink.py examples/device1.json usb:0
```

`usb:0` selects the first USB Bluetooth dongle. If you have more than one, use `usb:1`, `usb:2`, etc. The `device1.json` config gives the simulated device a fixed random address (`F0:F1:F2:F3:F4:F5`) and name (`Bumble`).

A successful start looks like:

```
D bumble.host: ### CONTROLLER -> HOST: HCI_COMMAND_COMPLETE_EVENT:
  command_opcode: HCI_LE_SET_ADVERTISE_ENABLE_COMMAND
  return_parameters:
    status: SUCCESS
```

The device is now advertising and waiting for a connection.

## Connecting from an Android phone

Soundscape targets Android 10+. ASHA device discovery is handled by the Android OS, not by a regular Bluetooth scan. To pair:

1. Go to **Settings → Accessibility → Hearing devices** (the exact path varies slightly between Android versions and OEM skins — search for "Hearing" in Settings if you can't find it).
2. Tap **Add device** or **Pair new hearing aid**.
3. The phone will scan and should find **Bumble**. Tap it to pair.

Once paired, the phone will reconnect automatically whenever the simulation is running and Soundscape will be able to stream audio to it via the ASHA profile.

## Troubleshooting

**Device not found during scan**
- Confirm the simulation started cleanly and reached the `HCI_LE_SET_ADVERTISE_ENABLE_COMMAND SUCCESS` line.
- Make sure you are looking in **Accessibility → Hearing devices**, not the regular Bluetooth settings — ASHA devices do not appear in the standard Bluetooth device list.
- If you previously attempted to pair before applying the patch above, clear any cached entry in the Android hearing device list and try again.

**`UNKNOWN_HCI_COMMAND_ERROR` on startup**
- Your dongle does not support extended advertising (it is BT 4.x). Make sure the patch above has been applied — `start_advertising` handles this automatically.

**`INVALID_COMMAND_PARAMETERS_ERROR` on `HCI_SET_EVENT_MASK_COMMAND` and immediate crash**
- Your dongle is Bluetooth 1.2 (or earlier) and cannot support ASHA. Replace it with a BT 4.0+ LE-capable dongle.

**`usb:0` picks the wrong device**
- Run `lsusb` to list USB devices and identify which bus/device number your Bluetooth dongle is. Then use `usb:<index>` to select it (Bumble numbers dongles starting from 0 in the order they appear on the bus).
