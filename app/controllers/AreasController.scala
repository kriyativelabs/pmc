package controllers

import javax.inject.Inject

import helpers.enums.UserType
import helpers.json.AreaSerializer
import helpers.{CommonUtil, ResponseHelper}
import models.{Area, Areas}
import play.api._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import security.{IsAuthenticated, PermissionCheckAction}

class AreasController @Inject()(implicit val messagesApi: MessagesApi, implicit val mail:MailerClient) extends Controller with AreaSerializer with CommonUtil with ResponseHelper {
  val logger = Logger(this.getClass)

  def create() = (IsAuthenticated andThen PermissionCheckAction(UserType.OWNER))(parse.json) { implicit request =>
    request.body.validate[Area].fold(
      errors => badRequest(errors.mkString),
      area => {
        val newArea = if(request.user.userType != UserType.ADMIN) area.copy(companyId = request.user.companyId) else area
        Areas.insert(newArea) match {
          case Left(e) =>  failed(s"Area with code:${area.code} already exists!")
          case Right(id) => created (Some (newArea), s"Successfully created new Area with code:${newArea.code}")
        }
      }
    )
  }

  def find(id: Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT)) { implicit request =>
    val area = if(request.user.userType != UserType.ADMIN) Areas.findById(id.toInt,Some(request.user.companyId)) else Areas.findById(id.toInt)
    if (area.isDefined) ok(Json.toJson(area), "Area details") else notFound(s"Area with $id not found")
  }

  def all() = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT)) { implicit request =>
    val areaList = if(request.user.userType != UserType.ADMIN ) Areas.getAll(Some(request.user.companyId)) else Areas.getAll()
    ok(Json.toJson(areaList), "List of areas")
  }

  def delete(id: Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.OWNER)) { implicit request =>
    Areas.delete(id.toInt,request.user.companyId) match {
      case Left(e) => failed(e)
      case Right(msg) => ok(None,s"Successfully deleted Area!")
    }
  }

  def update(id:Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.OWNER))(parse.json) { implicit request =>
    request.body.validate[Area].fold(
      errors => badRequest(errors.mkString),
      area => {
        if(!area.id.isDefined || (area.id.isDefined && id != area.id.get)) validationError(area,"Id provided in url and data are not equal")
        else {
          val newArea = area.copy(companyId = request.user.companyId)
          Areas.update(newArea) match {
            case Left(e) => failed(s"Area with code:${area.code} already exists!")
            case Right(r) => ok(Some(newArea), s"Updated area ${newArea.name} with code:${newArea.code}")
          }
        }
      }
    )
  }

}
