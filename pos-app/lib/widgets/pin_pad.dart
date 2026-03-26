import 'package:flutter/material.dart';

class PinPad extends StatelessWidget {
  const PinPad({
    required this.minPinLength,
    required this.maxPinLength,
    required this.pinLength,
    required this.loading,
    required this.onDigitTap,
    required this.onBackspace,
    required this.onSubmit,
    super.key,
  });

  final int minPinLength;
  final int maxPinLength;
  final int pinLength;
  final bool loading;
  final ValueChanged<String> onDigitTap;
  final VoidCallback onBackspace;
  final VoidCallback? onSubmit;

  @override
  Widget build(BuildContext context) {
    final buttons = <_PadButton>[
      for (var i = 1; i <= 9; i++)
        _PadButton(label: '$i', onTap: () => onDigitTap('$i')),
      _PadButton(
        icon: Icons.backspace_outlined,
        onTap: onBackspace,
      ),
      _PadButton(label: '0', onTap: () => onDigitTap('0')),
      _PadButton(
        icon: Icons.check_rounded,
        highlighted: true,
        disabled: onSubmit == null || loading,
        onTap: onSubmit ?? () {},
      ),
    ];

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFF0E1726),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: const Color(0xFF1D2A42)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'PIN keypad',
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 6),
          Text(
            'Enter your $maxPinLength-digit PIN and tap check to continue.',
            style: const TextStyle(color: Colors.white70),
          ),
          const SizedBox(height: 16),
          Expanded(
            child: GridView.builder(
              physics: const NeverScrollableScrollPhysics(),
              itemCount: buttons.length,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                mainAxisSpacing: 10,
                crossAxisSpacing: 10,
                childAspectRatio: 1.18,
              ),
              itemBuilder: (context, index) {
                final button = buttons[index];
                return FilledButton(
                  onPressed: button.disabled || loading ? null : button.onTap,
                  style: FilledButton.styleFrom(
                    backgroundColor: button.highlighted
                        ? const Color(0xFF1FE4FF)
                        : const Color(0xFF152238),
                    foregroundColor: button.highlighted
                        ? const Color(0xFF08101D)
                        : Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(24),
                    ),
                  ),
                  child: button.icon != null
                      ? Icon(button.icon, size: 30)
                      : Text(
                          button.label ?? '',
                          style: const TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                );
              },
            ),
          ),
          const SizedBox(height: 12),
          Text(
            pinLength >= minPinLength
                ? 'PIN ready to submit.'
                : 'PIN must be at least $minPinLength digits.',
            style: const TextStyle(color: Colors.white70),
          ),
        ],
      ),
    );
  }
}

class _PadButton {
  const _PadButton({
    this.label,
    this.icon,
    this.highlighted = false,
    this.disabled = false,
    required this.onTap,
  });

  final String? label;
  final IconData? icon;
  final bool highlighted;
  final bool disabled;
  final VoidCallback onTap;
}
