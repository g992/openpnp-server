package org.openpnp.machine.reference.driver;

import org.simpleframework.xml.Root;

/**
 * Драйвер G-code под Klipper с преднастроенными параметрами и типом
 * коммуникаций klipper.
 */
@Root
public class KlipperGcodeDriver extends GcodeDriver {
    public KlipperGcodeDriver() {
        setName("Klipper G-code Driver");
        setCommunicationsType(AbstractReferenceDriver.CommunicationsType.klipper);
        // Настройка команд/регексов через карту команд драйвера
        setCommand(null, CommandType.COMMAND_CONFIRM_REGEX, "^ok.*");
        setCommand(null, CommandType.COMMAND_ERROR_REGEX, "^(?:!!|error:).*");
        setCommand(null, CommandType.HOME_COMMAND, "G28 ; Home all axes");
        setCommand(null, CommandType.GET_POSITION_COMMAND, "M114");
    }
}
