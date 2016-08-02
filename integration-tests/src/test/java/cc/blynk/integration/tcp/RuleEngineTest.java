package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.eventor.Rule;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.Mail;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.Notify;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPin;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.Wait;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.BaseCondition;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.Between;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.Equal;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.GreaterThan;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.GreaterThanOrEqual;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.LessThan;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.LessThanOrEqual;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.NotBetween;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.NotEqual;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Response.OK;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RuleEngineTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    private static Rule buildRule(String s) {
        //example "if V1 > 37 then setpin V2 123"

        String[] splitted = s.split(" ");

        //"V1"
        Pin triggerPin = parsePin(splitted[1]);
                                                       //>                               37
        BaseCondition ifCondition = resolveCondition(splitted[2], Double.parseDouble(splitted[3]));

                                            //setpin                V2            123
        BaseAction action = resolveAction(splitted[5], parsePin(splitted[6]), splitted[7]);

        return new Rule(triggerPin, ifCondition, new BaseAction[] { action });
    }

    private static Pin parsePin(String pinString) {
        PinType pinType = PinType.getPinType(pinString.charAt(0));
        byte pin = Byte.parseByte(pinString.substring(1));
        return new Pin(pin, pinType);
    }

    private static BaseAction resolveAction(String action, Pin pin, String value) {
        switch (action) {
            case "setpin" :
                return new SetPin(pin, value);
            case "wait" :
                return new Wait();
            case "notify" :
                return new Notify();

            default: throw new RuntimeException("Not supported action. " + action);
        }
    }

    private static BaseCondition resolveCondition(String conditionString, double value) {
        switch (conditionString) {
            case ">" :
                return new GreaterThan(value);
            case ">=" :
                return new GreaterThanOrEqual(value);
            case "<" :
                return new LessThan(value);
            case "<=" :
                return new LessThanOrEqual(value);
            case "=" :
                return new Equal(value);
            case "!=" :
                return new NotEqual(value);

            default: throw new RuntimeException("Not supported operation. " + conditionString);
        }
    }

    private static Eventor oneRuleEventor(String ruleString) {
        Rule rule = buildRule(ruleString);
        return new Eventor(new Rule[] {rule});
    }

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
        this.appServer = new AppServer(holder).start(transportTypeHolder);
        this.clientPair = initAppAndHardPair("user_profile_json_3_dashes.txt");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void printAllInJson() throws Exception {
        Eventor tempEventor = oneRuleEventor("if v1 != 37 then setpin v2 123");
        //replace with between
        tempEventor.rules[0].condition = new Between(10, 12);

        Eventor tempEventor2 = oneRuleEventor("if v1 != 37 then setpin v2 123");
        //replace with between
        tempEventor2.rules[0].condition = new NotBetween(10, 12);


        Eventor[] eventors = new Eventor[]{
                oneRuleEventor("if v1 > 37 then setpin v2 123"),
         oneRuleEventor("if v1 >= 37 then setpin v2 123"),
        oneRuleEventor("if v1 <= 37 then setpin v2 123"),
        oneRuleEventor("if v1 = 37 then setpin v2 123"),
        oneRuleEventor("if v1 < 37 then setpin v2 123"),
         oneRuleEventor("if v1 != 37 then setpin v2 123"),
                tempEventor,
                tempEventor2
        };

        for (Eventor eventor : eventors) {
            System.out.println(JsonParser.mapper.writeValueAsString(eventor));
        }

        Pin pin = new Pin(1, PinType.VIRTUAL);

        BaseAction[] actions = new BaseAction[] {
                new SetPin(pin, "pinValuetoSEt"),
                new Wait(360, new SetPin(pin, "pinValueToSet")),
                new Notify("Hello!!!"),
                new Mail("Hello mail")
        };

        for (BaseAction action : actions) {
            System.out.println(JsonParser.mapper.writeValueAsString(action));
        }
    }

    @Test
    public void testSimpleRule1() throws Exception {
        Eventor eventor = oneRuleEventor("if v1 > 37 then setpin v2 123");

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 1 38");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 38"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));
    }

    @Test
    public void testSimpleRule2() throws Exception {
        Eventor eventor = oneRuleEventor("if v1 >= 37 then setpin v2 123");

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 1 37");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 37"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));
    }

    @Test
    public void testSimpleRule3() throws Exception {
        Eventor eventor = oneRuleEventor("if v1 <= 37 then setpin v2 123");

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 1 37");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 37"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));
    }

    @Test
    public void testSimpleRule4() throws Exception {
        Eventor eventor = oneRuleEventor("if v1 = 37 then setpin v2 123");

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 1 37");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 37"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));
    }

    @Test
    public void testSimpleRule5() throws Exception {
        Eventor eventor = oneRuleEventor("if v1 < 37 then setpin v2 123");

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 1 36");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 36"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));
    }

    @Test
    public void testSimpleRule6() throws Exception {
        Eventor eventor = oneRuleEventor("if v1 != 37 then setpin v2 123");

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 1 36");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 36"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));
    }

    @Test
    public void testSimpleRule7() throws Exception {
        Eventor eventor = oneRuleEventor("if v1 != 37 then setpin v2 123");

        //replace with between
        eventor.rules[0].condition = new Between(10, 12);

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 1 11");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 11"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));
    }

    @Test
    public void testSimpleRule8() throws Exception {
        Eventor eventor = oneRuleEventor("if v1 != 37 then setpin v2 123");

        //replace with between
        eventor.rules[0].condition = new NotBetween(10, 12);

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 1 9");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 9"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));
    }



    @Test
    public void testSimpleRuleCreateUpdateConditionWorks() throws Exception {
        Eventor eventor = oneRuleEventor("if v1 >= 37 then setpin v2 123");

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 1 37");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 37"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));

        eventor = oneRuleEventor("if v1 >= 37 then setpin v2 124");
        clientPair.appClient.send("updateWidget 1\0" + JsonParser.mapper.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        clientPair.hardwareClient.send("hardware vw 1 36");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1 vw 1 36"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 124"))));

        clientPair.hardwareClient.send("hardware vw 1 37");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("1 vw 1 37"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 124"))));
    }


}
