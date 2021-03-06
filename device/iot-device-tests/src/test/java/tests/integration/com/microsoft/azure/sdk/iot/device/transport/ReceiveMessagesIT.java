// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package tests.integration.com.microsoft.azure.sdk.iot.device.transport;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.service.Device;
import com.microsoft.azure.sdk.iot.service.IotHubServiceClientProtocol;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.ServiceClient;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tests.integration.com.microsoft.azure.sdk.iot.device.DeviceConnectionString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReceiveMessagesIT
{
    public static Map<String, String> messageProperties = new HashMap<>(3);

    private final static String SET_MINIMUM_POLLING_INTERVAL = "SetMinimumPollingInterval";
    private final static Long ONE_SECOND_POLLING_INTERVAL = 1000L;

    private static String iotHubonnectionStringEnvVarName = "IOTHUB_CONNECTION_STRING";
    private static String iotHubConnectionString = "";
    private static RegistryManager registryManager;
    private static Device deviceHttps;
    private static Device deviceAmqps;
    private static Device deviceMqtt;

    private static ServiceClient serviceClient;

    // How much to wait until receiving a message from the server, in milliseconds
    private Integer receiveTimeout = 60000;

    @BeforeClass
    public static void setUp() throws Exception
    {
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet())
        {
            if (envName.equals(iotHubonnectionStringEnvVarName.toString()))
            {
                iotHubConnectionString = env.get(envName);
            }
        }

        registryManager = RegistryManager.createFromConnectionString(iotHubConnectionString);
        String uuid = UUID.randomUUID().toString();
        String deviceIdHttps = "java-device-client-e2e-test-https".concat("-" + uuid);
        String deviceIdAmqps = "java-device-client-e2e-test-amqps".concat("-" + uuid);
        String deviceIdMqtt = "java-device-client-e2e-test-mqtt".concat("-" + uuid);

        deviceHttps = Device.createFromId(deviceIdHttps, null, null);
        deviceAmqps = Device.createFromId(deviceIdAmqps, null, null);
        deviceMqtt = Device.createFromId(deviceIdMqtt, null, null);

        registryManager.addDevice(deviceHttps);
        registryManager.addDevice(deviceAmqps);
        registryManager.addDevice(deviceMqtt);

        messageProperties = new HashMap<>(3);
        messageProperties.put("name1", "value1");
        messageProperties.put("name2", "value2");
        messageProperties.put("name3", "value3");

        serviceClient = ServiceClient.createFromConnectionString(iotHubConnectionString, IotHubServiceClientProtocol.AMQPS);
        serviceClient.open();
    }

    @AfterClass
    public static void TearDown() throws IOException, IotHubException
    {
        serviceClient.close();
        registryManager.removeDevice(deviceHttps.getDeviceId());
        registryManager.removeDevice(deviceAmqps.getDeviceId());
        registryManager.removeDevice(deviceMqtt.getDeviceId());
    }

    @Test
    public void ReceiveMessagesOverHttpsIncludingProperties() throws Exception
    {
        DeviceClient client = new DeviceClient(DeviceConnectionString.get(iotHubConnectionString, deviceHttps), IotHubClientProtocol.HTTPS);
        client.setOption(SET_MINIMUM_POLLING_INTERVAL, ONE_SECOND_POLLING_INTERVAL);
        client.open();

        try
        {
            Success messageReceived = new Success();
            com.microsoft.azure.sdk.iot.device.MessageCallback callback = new MessageCallback();
            client.setMessageCallback(callback, messageReceived);

            String messageString = "Java service e2e test message to be received over Https protocol";
            com.microsoft.azure.sdk.iot.service.Message serviceMessage = new com.microsoft.azure.sdk.iot.service.Message(messageString);
            serviceMessage.setProperties(messageProperties);
            String deviceId = deviceHttps.getDeviceId();
            serviceClient.send(deviceId, serviceMessage);


            Integer waitDuration = 0;
            while(true)
            {
                waitDuration += 100;
                if (messageReceived.getResult() || waitDuration > receiveTimeout)
                {
                    break;
                }
                Thread.sleep(100);
            }

            if (waitDuration > receiveTimeout)
            {
                Assert.fail("Receiving messages over HTTPS protocol timed out.");
            }

            if (!messageReceived.getResult())
            {
                Assert.fail("Receiving message over HTTPS protocol failed");
            }
        }
        catch (Exception e)
        {
            Assert.fail("Receiving message over HTTPS protocol failed");
        }

        Thread.sleep(200);
        client.closeNow();
    }

    @Test
    public void ReceiveMessagesOverAmqpsIncludingProperties() throws Exception
    {
        DeviceClient client = new DeviceClient(DeviceConnectionString.get(iotHubConnectionString, deviceAmqps), IotHubClientProtocol.AMQPS);
        client.open();

        try
        {
            Success messageReceived = new Success();
            com.microsoft.azure.sdk.iot.device.MessageCallback callback = new MessageCallback();
            client.setMessageCallback(callback, messageReceived);

            String messageString = "Java service e2e test message to be received over Amqps protocol";
            com.microsoft.azure.sdk.iot.service.Message serviceMessage = new com.microsoft.azure.sdk.iot.service.Message(messageString);
            serviceMessage.setProperties(messageProperties);
            String deviceId = deviceAmqps.getDeviceId();
            serviceClient.send(deviceId, serviceMessage);

            Integer waitDuration = 0;
            while(true)
            {
                waitDuration += 100;
                if (messageReceived.getResult() || waitDuration > receiveTimeout)
                {
                    break;
                }
                Thread.sleep(100);
            }

            if (waitDuration > receiveTimeout)
            {
                Assert.fail("Receiving messages over AMQPS protocol timed out.");
            }

            if (!messageReceived.getResult())
            {
                Assert.fail("Receiving message over AMQPS protocol failed");
            }
        }
        catch (Exception e)
        {
            Assert.fail("Receiving message over AMQPS protocol failed");
        }

        Thread.sleep(200);
        client.closeNow();
    }

    @Test
    public void ReceiveMessagesOverMqtt() throws Exception
    {
        DeviceClient client = new DeviceClient(DeviceConnectionString.get(iotHubConnectionString, deviceMqtt), IotHubClientProtocol.MQTT);
        client.open();

        try
        {
            Success messageReceived = new Success();
            com.microsoft.azure.sdk.iot.device.MessageCallback callback = new MessageCallbackMqtt();
            client.setMessageCallback(callback, messageReceived);

            String messageString = "Java service e2e test message to be received over Mqtt protocol";
            com.microsoft.azure.sdk.iot.service.Message serviceMessage = new com.microsoft.azure.sdk.iot.service.Message(messageString);
            String deviceId = deviceMqtt.getDeviceId();
            serviceClient.send(deviceId, serviceMessage);

            Integer waitDuration = 0;
            while(true)
            {
                waitDuration += 100;
                if (messageReceived.getResult() || waitDuration > receiveTimeout)
                {
                    break;
                }
                Thread.sleep(100);
            }

            if (waitDuration > receiveTimeout)
            {
                Assert.fail("Receiving messages over MQTT protocol timed out.");
            }

            if (!messageReceived.getResult())
            {
                Assert.fail("Receiving message over MQTT protocol failed");
            }
        }
        catch (Exception e)
        {
            Assert.fail("Receiving message over MQTT protocol failed");
        }

        Thread.sleep(200);
        client.closeNow();
    }

    protected static class MessageCallback implements com.microsoft.azure.sdk.iot.device.MessageCallback
    {
        public IotHubMessageResult execute(Message msg, Object context)
        {
            Boolean resultValue = true;
            HashMap<String, String> messageProperties = (HashMap<String, String>) ReceiveMessagesIT.messageProperties;
            Success messageReceived = (Success)context;
            MessageProperty[] messagePropertiesFromService = msg.getProperties();
            if (messageProperties.size() != messagePropertiesFromService.length)
            {
                resultValue = false;
            }
            else
            {
                for (int i = 0; i < 3; i++)
                {
                    if (!messageProperties.containsKey(messagePropertiesFromService[i].getName())
                            || !messageProperties.containsValue(messagePropertiesFromService[i].getValue()))
                    {
                        resultValue = false;
                        break;
                    }
                }
            }
            messageReceived.setResult(resultValue);
            return IotHubMessageResult.COMPLETE;
        }
    }

    private class MessageCallbackMqtt implements com.microsoft.azure.sdk.iot.device.MessageCallback
    {
        public IotHubMessageResult execute(Message msg, Object context)
        {
            Success messageReceived = (Success)context;
            messageReceived.setResult(true);
            return IotHubMessageResult.COMPLETE;
        }
    }

    private class Success
    {
        public Boolean result = false;

        public void setResult(Boolean result)
        {
            this.result = result;
        }

        public Boolean getResult()
        {
            return this.result;
        }
    }
}
