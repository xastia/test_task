package com.xastia.test.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.xastia.test.R
import com.xastia.test.databinding.FragmentOnboardingBinding

/**
 * Один слайд онбордингу. Параметри передаються через arguments —
 * один XML-лейаут (fragment_onboarding.xml) обслуговує обидва слайди.
 */
class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val iconRes = args.getInt(ARG_ICON_RES)
        val cardBgRes = args.getInt(ARG_CARD_BG_RES)
        val iconTintRes = args.getInt(ARG_ICON_TINT_RES)
        val titleRes = args.getInt(ARG_TITLE_RES)
        val bodyRes = args.getInt(ARG_BODY_RES)

        binding.illustrationCard.setBackgroundResource(cardBgRes)
        binding.illustrationIcon.setImageResource(iconRes)
        binding.illustrationIcon.imageTintList =
            ContextCompat.getColorStateList(requireContext(), iconTintRes)
        binding.slideTitle.setText(titleRes)
        binding.slideBody.setText(bodyRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ICON_RES = "arg_icon_res"
        private const val ARG_CARD_BG_RES = "arg_card_bg_res"
        private const val ARG_ICON_TINT_RES = "arg_icon_tint_res"
        private const val ARG_TITLE_RES = "arg_title_res"
        private const val ARG_BODY_RES = "arg_body_res"

        fun newInstance(
            iconRes: Int,
            cardBgRes: Int,
            iconTintRes: Int,
            titleRes: Int,
            bodyRes: Int
        ): OnboardingFragment = OnboardingFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_ICON_RES, iconRes)
                putInt(ARG_CARD_BG_RES, cardBgRes)
                putInt(ARG_ICON_TINT_RES, iconTintRes)
                putInt(ARG_TITLE_RES, titleRes)
                putInt(ARG_BODY_RES, bodyRes)
            }
        }
    }
}
