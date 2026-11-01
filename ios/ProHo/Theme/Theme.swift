import SwiftUI
import UIKit

/// Brand palette from brand/README.md, matching
/// android/.../ui/theme/Theme.kt: ink navy for bars, gold accent darkened
/// for light mode and lightened for dark mode so both meet contrast.
enum Theme {
    static let ink = Color(hex: 0x0E1B2E)

    static let accent = Color(UIColor { traits in
        traits.userInterfaceStyle == .dark ? UIColor(hex: 0xF0B36B) : UIColor(hex: 0x9C6418)
    })
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(red: CGFloat((hex >> 16) & 0xFF) / 255,
                  green: CGFloat((hex >> 8) & 0xFF) / 255,
                  blue: CGFloat(hex & 0xFF) / 255,
                  alpha: 1)
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(uiColor: UIColor(hex: hex))
    }
}
