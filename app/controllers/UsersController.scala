package controllers

import helpers.enums.UserType
import helpers.json.UserSerializer
import helpers.{CommonUtil, ResponseHelper}
import models.{PasswordChange, LoginCase, Users, User}
import play.api.libs.json._
import security.{Authentication, IsAuthenticated, PermissionCheckAction}
import play.api._
import play.api.mvc._

object UsersController extends Controller with UserSerializer with CommonUtil with ResponseHelper {
  val logger = Logger(this.getClass)

  def create() = (IsAuthenticated andThen PermissionCheckAction(UserType.OWNER))(parse.json) { implicit request =>
    request.body.validate[User].fold(
      errors => BadRequest(errors.mkString),
      user => {
        val newUser = if(request.user.userType == UserType.OWNER) user.copy(companyId = request.user.companyId) else user
        Users.insert(newUser) match {
          case Left(e) =>  BadRequest(e)
          case Right(id) => created (Some (newUser), s"Created User with id:$id")
        }
      }
    )
  }

  def login() = Action(parse.json) { implicit request =>
    request.body.validate[LoginCase].fold(
      errors => BadRequest(errors.mkString),
      login => {
        Users.login(login) match {
          case Left(l) =>  unAuthorized("Authentication failed!")
          case Right(r) => {
            val token = Authentication.encryptAuthHeader(r.id.get, r.companyId, 3, UserType.withName(r.accountType))
            ok(Json.obj("token" -> token, "name" -> r.name), s"Successfully logged in!")
          }
        }
      }
    )
  }

  def updatePassword() = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT))(parse.json) { implicit request =>
    request.body.validate[PasswordChange].fold(
      errors => BadRequest(errors.mkString),
      passwordData => {
        val userId = request.user.userId
        Users.updatePassword(userId, passwordData.oldPassword, passwordData.newPassword) match {
          case Left(e) => validationError("Password is not updated!", e)
          case Right(r) => if (r == 0) failed("Password not updated! Old password not matching") else ok(Some("Password Updated Successfully"), s"Updated User with details$r")
        }
      }
    )
  }

  def find(id: Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.OWNER)) { implicit request =>
    val userDao = if(request.user.userType == UserType.OWNER) Users.findById(id.toInt,Some(request.user.companyId)) else Users.findById(id.toInt)
    if (userDao.isDefined) ok(Json.toJson(userDao), "User details") else notFound(s"User with $id not found")
  }

  def all() = (IsAuthenticated andThen PermissionCheckAction(UserType.OWNER)) { implicit request =>
    val userList = if(request.user.userType == UserType.OWNER) Users.getAll(Some(request.user.companyId)) else Users.getAll()
    ok(Json.toJson(userList), "List of users")
  }

  def update(id:Int) = (IsAuthenticated andThen PermissionCheckAction(UserType.AGENT))(parse.json) { implicit request =>
    request.body.validate[User].fold(
      errors => BadRequest(errors.mkString),
      user => {
        if(!user.id.isDefined || (user.id.isDefined && id != user.id.get)) validationError(user,"Id provided in url and data are not equal")
        else {
          Users.update(user) match {
            case Left(e) => validationError(user, e)
            case Right(r) => ok(Some(user), s"Updated User with details" + user)
          }
        }
      }
    )
  }

}