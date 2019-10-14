package di

import di.ContextObjectRegistry.AlreadyInContextException
import di.ContextObjectRegistry.AlreadyInParentContextException
import di.ContextObjectRegistry.NotInContextException
import org.specs2.mutable.Specification

class ContextObjectRegistrySpecification extends Specification {

  val DriverJosh = Driver("Josh")

  "ContextObjectRegistry addComponentToContext" should {
    "add components to registry" in {
      val seat = DriverSeat(DriverJosh)
      seat.getSingletonObject[Driver] must_== DriverJosh
    }

    "throw error when adding 2 components of the same type" in {
      val seat = DriverSeat(DriverJosh)
      seat.addComponentToContext(Driver("Jon")) must throwAn[AlreadyInContextException]
    }

    "throw error when grand parent has registered a component of the same type" in {
      val seat = DriverSeat(DriverJosh)
      val car = Car(seat)
      val garage = Garage(car)
      garage.addComponentToContext(Color(29))
      seat.addComponentToContext(Color(42)) must throwAn[AlreadyInContextException]
    }
  }

  "ContextObjectRegistry getSingletonObject" should {

    "throw an `NotInContextException` when no component of the given type is registered" in {
      val seat = DriverSeat(DriverJosh)
      seat.getSingletonObject[String] must throwAn[NotInContextException]
      seat.getSingletonObject[Color] must throwAn[NotInContextException]
    }

    "return component from parent's registry" in {
      val seat = DriverSeat(DriverJosh)
      val car = Car(seat)

      seat.getSingletonObject[Color] must throwAn[NotInContextException]

      val color = Color(23)
      car.addComponentToContext(color)
      seat.getSingletonObject[Color] must_== color
    }

    "NOT return component from child's context" in {
      val seat = DriverSeat(DriverJosh)
      val car = Car(seat)

      seat.addComponentToContext(Color(23))
      car.getSingletonObject[Color] must throwAn[NotInContextException]
    }

    "retrieve typed collections from context" in {
      val seat = DriverSeat(DriverJosh)
      seat.addComponentToContext(List(1, 2, 3))
      seat.addComponentToContext(List("a", "b"))
      seat.getSingletonObject[List[Int]] must_== List(1, 2, 3)
      seat.getSingletonObject[List[String]] must_== List("a", "b")
    }

    "NOT return components of a subtype class" in {
      val seat = DriverSeat(DriverJosh)
      seat.addComponentToContext(FuzzyColor(22))
      seat.getSingletonObject[Color] must throwAn[NotInContextException]
    }
  }

  "ContextObjectRegistry shareContextToChild" should {
    "throw an error if child has a component of the current context" in {
      val seat = DriverSeat(DriverJosh)
      seat.addComponentToContext(Color(23))
      val grandParent = GrandParent()
      grandParent.addComponentToContext(Color(11))
      grandParent.addComponentToContext(seat)
      grandParent.shareContextToChild(seat) must throwAn[AlreadyInParentContextException]
    }
  }
}


class Color(num: Int)
object Color {
  def apply(num: Int): Color = new Color(num)
}

case class FuzzyColor(num: Int) extends Color(num * 3)

case class Driver(name: String)
case class DriverSeat(driver: Driver) extends ContextObjectRegistry {
  addComponentToContext(driver)
}

case class Car(driverSeat: DriverSeat) extends ContextObjectRegistry {
  addComponentToContext(driverSeat)
  shareContextToChild(driverSeat)
}
case class Garage(car: Car) extends ContextObjectRegistry {
  addComponentToContext(car)
  shareContextToChild(car)
}
case class GrandParent() extends ContextObjectRegistry {}
