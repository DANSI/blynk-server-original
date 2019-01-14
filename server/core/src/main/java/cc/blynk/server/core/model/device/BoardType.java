package cc.blynk.server.core.model.device;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Used mostly to minimize memory footprint used by boardType strings.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 08.06.18.
 */
public enum BoardType {

    ESP8266("ESP8266"),
    Arduino_UNO("Arduino UNO"),
    NodeMCU("NodeMCU"),
    Raspberry_Pi_3_B("Raspberry Pi 3 B"),
    WeMos_D1_mini("WeMos D1 mini"),
    Arduino_Nano("Arduino Nano"),
    Arduino_Mega("Arduino Mega"),
    ESP32_Dev_Board("ESP32 Dev Board"),
    WeMos_D1("WeMos D1"),
    Generic_Board("Generic Board"),
    Raspberry_Pi_2_A_B("Raspberry Pi 2/A+/B+"),
    Particle_Photon("Particle Photon"),
    Arduino_MKR1000("Arduino MKR1000"),
    Arduino_101("Arduino 101"),
    Arduino_Yun("Arduino Yun"),
    Raspberry_Pi_A_B_v2("Raspberry Pi A/B (Rev2)"),
    Arduino_Pro_Mini("Arduino Pro Mini"),
    Arduino_Leonardo("Arduino Leonardo"),
    Raspberry_Pi_B_v1("Raspberry Pi B (Rev1)"),
    Arduino_Due("Arduino Due"),
    SparkFun_Blynk_Board("SparkFun Blynk Board"),
    Orange_Pi("Orange Pi"),
    BBC_Microbit("BBC Micro:bit"),
    Arduino_Mini("Arduino Mini"),
    Arduino_Micro("Arduino Micro"),
    Onion_Omega("Onion Omega"),
    Arduino_Pro_Micro("Arduino Pro Micro"),
    Particle_Core("Particle Core"),
    SparkFun_ESP8266_Thing("SparkFun ESP8266 Thing"),
    STM32F103C_Blue_Pill("STM32F103C Blue Pill"),
    WiPy("WiPy"),
    Particle_Electron("Particle Electron"),
    Arduino_Zero("Arduino Zero"),
    Intel_Edison("Intel Edison"),
    Teensy_3("Teensy 3"),
    LinkIt_ONE("LinkIt ONE"),
    NanoPi("NanoPi"),
    LightBlue_Bean("LightBlue Bean"),
    Intel_Galileo("Intel Galileo"),
    RedBearLab_BLE_Nano("RedBearLab BLE Nano"),
    RedBear_Duo("RedBear Duo"),
    TI_CC3200_LaunchXL("TI CC3200-LaunchXL"),
    Digistump_Oak("Digistump Oak"),
    Seeed_Wio_Link("Seeed Wio Link"),
    TI_Tiva_C_Connected("TI Tiva C Connected"),
    Samsung_ARTIK_5("Samsung ARTIK 5"),
    Microduino_CoreUSB("Microduino CoreUSB"),
    Espruino_Pico("Espruino Pico"),
    TinyDuino("TinyDuino"),
    Microduino_Core_plus("Microduino Core+"),
    chipKIT_Uno32("chipKIT Uno32"),
    The_AirBoard("The AirBoard"),
    Microduino_Core("Microduino Core"),
    Simblee("Simblee"),
    LeMaker_Banana_Pro("LeMaker Banana Pro"),
    Wildfire_v2("Wildfire v2"),
    LightBlue_Bean_plus("LightBlue Bean+"),
    SparkFun_Photon_RedBoard("SparkFun Photon RedBoard"),
    Microduino_CoreRF("Microduino CoreRF"),
    RedBearLab_CC3200_Mini("RedBearLab CC3200/Mini"),
    Bluz("Bluz"),
    LeMaker_Guitar("LeMaker Guitar"),
    panStamp_esp_output("panStamp esp-output"),
    Digistump_Digispark("Digistump Digispark"),
    RedBearLab_Blend_Micro("RedBearLab Blend Micro"),
    TI_LM4F120_LaunchPad("TI LM4F120 LaunchPad"),
    Wildfire_v3("Wildfire v3"),
    Wildfire_v4("Wildfire v4"),
    Konekt_Dash_Pro("Konekt Dash Pro");

    public final String label;

    private static final BoardType[] values = values();

    BoardType(String label) {
        this.label = label;
    }

    @JsonCreator
    public static BoardType fromLabel(String label) {
        for (BoardType type : values) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        return Generic_Board;
    }

    @JsonValue
    String label() {
        return label;
    }
}
