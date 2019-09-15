import time
import sys
import socket as sk

def showErrorException(infoError):
    """ Imprimir errores generados  """
    print ("---> Error::: ", infoError[0])
    print ("---> Error Type::: ", infoError[1])

# Se lee del Socket los datos del sensor
host =  "192.168.1.195"

port1 = 60000
port2 = 60001
port3 = 60002

ADDR1 = (str(host), int(port1))
ADDR2 = (str(host), int(port2))
ADDR3 = (str(host), int(port3))

print("---> Set parameter socket: [host:'" + host + "', port's: [" + str(port1) + "," + str(port2) + "]")

sock1 = sk.socket(sk.AF_INET, sk.SOCK_STREAM)
sock2 = sk.socket(sk.AF_INET, sk.SOCK_STREAM)
sock3 = sk.socket(sk.AF_INET, sk.SOCK_STREAM)

#################################################
# OPTIONAL::: se controla la actividad del sensor (connect or unconnet)
sw = True

try:
    sock1.connect(ADDR1)
    sock2.connect(ADDR2)
    sock3.connect(ADDR3)
except:
    print("---> [Err initial] No read data from Socket")
    showErrorException(sys.exc_info())
    sw = False

while True:
    try:
        if sw is False:
            print("---> Reconnect to Socket PHAT-SIM...")
            try:
                sock1.close()
                sock2.close()
                sock3.close()
                sock1.connect(ADDR1)
                sock2.connect(ADDR2)
                sock3.connect(ADDR3)
                sw = True
            except:
                print ("---> ...Wait Sensor Accelerometer of PHAT-SIM")
                sw = False
        else:
            print("---> Read Data Sensor...")
            for data1 in sock1.makefile('r'):
                for data2 in sock2.makefile('r'):
                    for data3 in sock3.makefile('r'):
                        if len(data1.split(";")) == 7 and len(data2.split(";")) == 7 and len(data3.split(";")) == 7:
                            # Se caupturan los datos del sensor, y se crea un (list:list) con el conjunto de ellos
                            data1 = data1.replace("\n", "")
                            data2 = data2.replace("\n", "")
                            data3 = data3.replace("\n", "")
                            print("Sensor 1:", data1, "Sensor 2:", data2, "Sensor 3:", data3)

    except:
        showErrorException(sys.exc_info())
        sw = False

    time.sleep(1)