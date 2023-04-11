package Transport_Staff

abstract  class Transport {
    abstract val capacity:Int
    //Чем больше вместимость, тем больше кузов -> время на перемещение товара внутри кузова (при загрузке/разгрузке) тоже больше.
    //Введем соответствующий коэффициент, который будет влиять на разгрузку/загрузку товара
    abstract val timeFactorOfLoadAndUnload:Double
}