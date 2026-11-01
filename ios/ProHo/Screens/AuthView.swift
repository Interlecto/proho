import SwiftUI
import ProHoKit

/// Log in / create account. Same flow as android/.../AuthScreen.kt:
/// registering logs straight in afterwards.
struct AuthView: View {
    private enum Mode { case login, register }

    @Environment(SessionModel.self) private var session
    @State private var mode = Mode.login
    @State private var displayName = ""
    @State private var email = ""
    @State private var password = ""
    @State private var errorKey: String?
    @State private var busy = false

    var body: some View {
        Form {
            Section {
                Label("no_community", systemImage: "building.2")
                    .foregroundStyle(.secondary)
            }
            Section(header: Text(titleKey)) {
                if mode == .register {
                    TextField("name_label", text: $displayName)
                        .textContentType(.name)
                }
                TextField("email_label", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                SecureField("password_label", text: $password)
                    .textContentType(mode == .login ? .password : .newPassword)
            }
            if let errorKey {
                Section {
                    Text(LocalizedStringKey(errorKey)).foregroundStyle(.red)
                }
            }
            Section {
                Button(action: submit) {
                    HStack {
                        Text(titleKey)
                        if busy { Spacer(); ProgressView() }
                    }
                }
                .disabled(busy)
                Button(switchModeKey) {
                    mode = mode == .login ? .register : .login
                    errorKey = nil
                }
            }
        }
    }

    private var titleKey: LocalizedStringKey {
        mode == .login ? "login_title" : "register_title"
    }

    private var switchModeKey: LocalizedStringKey {
        mode == .login ? "no_account_prompt" : "has_account_prompt"
    }

    private func submit() {
        errorKey = nil
        busy = true
        Task {
            defer { busy = false }
            do {
                if mode == .register {
                    _ = try await session.api.register(email: email, password: password, displayName: displayName)
                }
                session.didLogIn(try await session.api.login(email: email, password: password))
            } catch let error as APIError {
                errorKey = error.messageKey
            } catch {
                errorKey = "error_unknown"
            }
        }
    }
}
