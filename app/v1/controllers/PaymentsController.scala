package v1.controllers

import javax.inject.Inject

import helpers.enums.UserType
import v1.helpers.json.PaymentSerializer
import v1.helpers.{CommonUtil, ResponseHelper}
import v1.models.{Payment, Payments}
import play.api._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import security.{IsAuthenticated, PermissionCheckAction}
import utils.CommonUtils

class PaymentsController @Inject()(implicit val messagesApi: MessagesApi, implicit val mail:MailerClient) extends Controller with PaymentSerializer with CommonUtil with ResponseHelper {
  val logger = Logger(this.getClass)

  def create() = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT))(parse.json) { implicit request =>
    request.body.validate[Payment].fold(
      errors => badRequest(errors.mkString),
      payment => {
        implicit val loggedInUser = request.user
        val newPayment = payment.copy(companyId = request.user.companyId, agentId = request.user.userId)
        Payments.insert(newPayment) match {
          case Left(e) => badRequest(e)
          case Right(id) => created(Some(newPayment), s"Payment of amount:${newPayment.paidAmount} has been received!")
        }
      }
    )
  }

  def find(id: Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT)) { implicit request =>
    implicit val loggedInUser = request.user
    val payment = Payments.findById(id.toInt)
    if (payment.isDefined) ok(Json.toJson(payment), "Payment details") else notFound(s"Payment with $id not found")
  }

  def all() = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT)) { implicit request =>
    implicit val loggedInUser = request.user
    val paymentList = Payments.getAll(paginationAttributes._4.getOrElse(1), paginationAttributes._3.getOrElse(20))
    ok(Json.toJson(paymentList), "List of payments")
  }

  def findByCustomerId(id: Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT)) { implicit request =>
    implicit val loggedInUser = request.user
    val paymentList = Payments.findByCustId(id.toInt)
    ok(Json.toJson(paymentList), "List Payment details")
  }

  def customerCredits(id:Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT)) { implicit request =>
    implicit val loggedInUser = request.user
    val credits = Payments.findCreditsByCustomerId(id)
    ok(Json.toJson(credits), "Customer Credits")
  }
  def findByAgentId(id: Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT)) { implicit request =>
    implicit val loggedInUser = request.user
    val paymentList = Payments.findByAgentId(id.toInt)
    ok(Json.toJson(paymentList), "List Payment details")
  }

  def findByAgentIdToday(id: Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT)) { implicit request =>
    implicit val loggedInUser = request.user
    val paymentList = Payments.findByAgentIdToday(id.toInt)
    ok(Json.toJson(paymentList), "List Payment details")
  }

  def search() = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT)) { implicit request =>
    implicit val loggedInUser = request.user
    val startDate = CommonUtils.string2Date(request.getQueryString("startdate").get).get
    val endDate = CommonUtils.string2Date(request.getQueryString("enddate").get).get

    val payments = Payments.searchByDateRange(startDate, endDate)
    ok(Json.toJson(payments), "List of Payments")
  }
}