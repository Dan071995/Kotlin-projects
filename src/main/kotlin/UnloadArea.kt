import Goods_Staff.ListsWithGoods
import Transport_Staff.Truck
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

/** Создадим зону РАЗГРУЗКИ, по сути она представляет несколько корутин (каждая отдельная корутина это порт разгрузки),
которые принимают загруженные грузовики из 1-ого канала (генератор полных грузовиков) и добавляют товары из этих грузовиков
в общий список товаров распределительного центра (данный список находится в объекте DistributionCenter). */

object UnloadArea{
    private var truckCounter = 0 //Счетчик разгруженных грузовиков
    val unloadPortJob = Job() // Джобы корутин, отвечающих за остановку программы. Этот Джоб отменяется после того, как
    //Порты разгрузки завершили свою работу. Изменение состояния данного джоба будет служить флагом для портов ЗАГРУЗКИ.
    //Как только порты РАЗГРУЗКИ остановленны, следует остановить порты ЗАГРУЗКИ

    init {  }
        suspend fun start() {

            coroutineScope { //ВНИМАНИЕ! РАНЬШЕ БЫЛ runBlocking {} а не coroutineScope{}
                val producer1 = generateFullTrucks() //запускаем генератор (канал 1) ЗАГРУЖЕННЫХ грузовиков

                //Остановим программу, как только будет обработано (разгружено) N грузовиков if (truckCounter == DistributionCenter.trucksNum).
                // runBlocking создает свой CoroutineContext(), который автоматически передается в корутины ПОРТОВ
                // repeat(DistributionCenter.unloadPortsNum) { createUnloadPort(it, producer1) }.
                // У корутины,которая отменяет производство грузовиков и печатает точки, пока идет завершение работы Портов, имеется свой CoroutineContext() (context = unloadPortJob),
                // который автоматически СКЛАДЫВАЕТСЯ с CoroutineContext() из CoroutineScope, который создал runBlocking. Если у данной
                //корутины будет CoroutineContext() из runBlocking (т.е не будет доп контекста context = unloadPortJob), то runBlocking не сможет завершиться и точки будут печататься бесконечно.
                //так как у данной корутины нет функции .join() то runBlocking не будет дожидаться выполнения ее CoroutineContext() и это позволит запустить следуюшие корутины, которые подписываются
                // на источник producer1 и отменяются автоматически после того как источник будет отменен (producer1.cancel()) и каждая из них закончит свою текущую работу. После того,
                //как завершаться корутины, разгружающие грузовики, завершиться runBlocking, и, соответственно, и эта корутина.
                launch(context = unloadPortJob) {
                    println("КОЛ-ВО РАЗГРУЖЕННЫХ ГРУЗОВИКОВ: $truckCounter")

                    while (currentCoroutineContext().job.isActive) {
                        delay(5)
                        if (truckCounter == DistributionCenter.trucksNum) {
                            println("\nКОЛ-ВО РАЗГРУЖЕННЫХ ГРУЗОВИКОВ: $truckCounter")
                            //DistributionCenter.centerGoodsList.sortBy{it.godName} //когда работа центра завершается, сортируем товары по местам
                            producer1.cancel() //останавливаем генерацию грузовиков
                            DistributionCenter.printProgress("ОЖИДАНИЕ ЗАВЕРЩЕНИЯ РАЗГРУЗКИ ПОСЛЕДНЕГО ГРУЗОВИКА В ПОРТУ")
                            //x.cancel() //этот код не сработает, так как его блокирует while из функции printProgress()
                        }
                    }
                }

                //Запускаем 3 порта РАЗГРУЗКИ (3 корутины), которые будут перемещать товар из грузовиков в хранилище центра
                //Эта функция повторяется N раз. Каждое ее повторение запускает корутину в данном coroutineScope{}. КАК ТОЛЬКО
                //БУДЕТ ОТМЕНЕН ИСТОЧНИК (producer1.cancel()) - данные корутины завершат свою работу и будут отмененый автоматически
                repeat(DistributionCenter.unloadPortsNum) { createUnloadPort(it, producer1) }
            }

            println(" ! DONE (UNload area) !") //После точек с прогрессом будет выведена эта надпись
            delay(10) //данная задержка нужна, чтобы после завершения разгрузки последний товар, добавленный в зону разгрузки успел переместиться на сортированный склад.
            unloadPortJob.complete()
            unloadPortJob.cancel() //Когда runBlocking завершается отменяем Джоб корутины, с добавленным контекстом. Данный джоб будет служить своего
            //рода флагом для работы центра ПОГРУЗКИ товаров (LoadArea). Как только порты РАЗГРУЗКИ остановлены, следует остановить
            //и порты загрузки (не присылать к ним новые грузовики)
            val emptyString = " ".padEnd(67, ' ')
            println("\n $emptyString СПИСОК ОСТАВШИХСЯ ТОВАРОВ В ЗОНЕ РАЗГРУЗКИ / ПОГРУЗКИ, ПОСЛЕ ЗАВЕРШЕНИЯ ПОГРУЗКИ: (${DistributionCenter.unsortedStorageOfGoods.size} шт): \n${DistributionCenter.unsortedStorageOfGoods}\n")
        }


//СОЗДАЕМ ГЕНЕРАТОР ЗАГРУЖЕННЫХ ГРУЗОВИКОВ И ФУНКЦИЮ СОЗДАНИЯ ПОРТА РАЗГРУЗКИ

    /** Функция, создающую поток (канал 1), который будет генерировать ЗАГРУЖЕННЫЕ ТОВАРАМИ грузовики для РАЗГРУЗКИ в центре */
    private fun CoroutineScope.generateFullTrucks() = produce(capacity = 20) {
        while (true) {
            //Генерируем рандомный тип грузовика, заполненный товарами из рандомной категории (Едой или Не едой)
            val randomTruckWithRandomGoods = DistributionCenter.createRandomTruck()
            randomTruckWithRandomGoods.fullTrackWithGoods(ListsWithGoods.allGoodsList.random())
            send(randomTruckWithRandomGoods) // отправляем загруженный грузовик в буфер, оттуда он попадет в свободный порт разгрузки
            delay(500) // wait 0.5s
        }
    }

    //Создадим функцию, которая создает порт РАЗГРУЗКИ, принимает и разгружает грузовик из канала 1:
    private fun CoroutineScope.createUnloadPort(id: Int, channel: ReceiveChannel<Truck>) = launch {

        for (fullTruck in channel) {
            println("\n                     ~UNLOAD PORT~ #${id+1}" +
                    "\n                    STARTED UNLOAD THE: ")
            fullTruck.printTrackInfo()
            truckCounter++

            fullTruck.goodsInTruckBack.forEach {
                delay( (it.timeForLoadAndUnload * fullTruck.timeFactorOfLoadAndUnload).toLong() ) //ждем пока этот товар добавиться на склад центра
                DistributionCenter.putElementsInUnsortedStorage(it) //Добавляем товар на склад
            }
        }
    }
}