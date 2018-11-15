package org.broadinstitute.consent.http.service.users.handler;

public class UserRoleHandlerException extends Exception{

        public UserRoleHandlerException() {
            super(); }

        public UserRoleHandlerException(String message) {
            super(message); }

        public UserRoleHandlerException(String message, Throwable cause) {
            super(message, cause); }

        public UserRoleHandlerException(Throwable cause) {
            super(cause); }
}
