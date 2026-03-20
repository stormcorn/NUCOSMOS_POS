import React from "react";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCreditCard, faTrash } from '@fortawesome/free-solid-svg-icons';

        const CartPanel = () => (
          <>
            <div className="w-80 lg:w-96 bg-black border-l border-gray-800 flex flex-col" id="cart-sidebar">
<div className="p-6 border-b border-gray-800">
<h2 className="text-xl font-bold text-neon-cyan glow-text">Current Order</h2>
<p className="text-gray-400 text-sm mt-1">Table #12</p>
</div>
<div className="flex-1 overflow-y-auto p-6">
{/* Cart Item 1 */}
<div className="cart-item bg-gray-800 rounded-xl p-4 mb-4">
<div className="flex justify-between items-start mb-3">
<div>
<h4 className="font-semibold">Premium Oolong Tea</h4>
<p className="text-gray-400 text-sm">$8.50</p>
</div>
<button className="text-red-400 hover:text-red-300">
<FontAwesomeIcon icon={faTrash} className="text-sm" />
</button>
</div>
<div className="space-y-3">
<div>
<label className="text-sm text-gray-300 block mb-1">Sugar Level</label>
<div className="flex space-x-2">
<button className="px-2 py-1 text-xs bg-gray-700 rounded">0%</button>
<button className="px-2 py-1 text-xs bg-neon-cyan text-black rounded">50%</button>
<button className="px-2 py-1 text-xs bg-gray-700 rounded">100%</button>
</div>
</div>
<div>
<label className="text-sm text-gray-300 block mb-1">Ice Level</label>
<div className="flex space-x-2">
<button className="px-2 py-1 text-xs bg-gray-700 rounded">No Ice</button>
<button className="px-2 py-1 text-xs bg-neon-cyan text-black rounded">Normal</button>
<button className="px-2 py-1 text-xs bg-gray-700 rounded">Extra</button>
</div>
</div>
</div>
<div className="flex justify-between items-center mt-4">
<div className="flex items-center space-x-3">
<button className="w-8 h-8 bg-gray-700 rounded-full flex items-center justify-center">-</button>
<span className="font-semibold">1</span>
<button className="w-8 h-8 bg-neon-cyan text-black rounded-full flex items-center justify-center">+</button>
</div>
<span className="font-bold text-neon-cyan">$8.50</span>
</div>
</div>
{/* Cart Item 2 */}
<div className="cart-item bg-gray-800 rounded-xl p-4 mb-4">
<div className="flex justify-between items-start mb-2">
<div>
<h4 className="font-semibold">AI Lecture Pass</h4>
<p className="text-gray-400 text-sm">Future of Machine Learning</p>
<p className="text-gray-400 text-sm">Dec 15, 2024 - 7:00 PM</p>
<p className="text-neon-purple font-semibold text-sm mt-1">$25.00</p>
</div>
<button className="text-red-400 hover:text-red-300">
<FontAwesomeIcon icon={faTrash} className="text-sm" />
</button>
</div>
<div className="flex justify-between items-center mt-4">
<div className="flex items-center space-x-3">
<button className="w-8 h-8 bg-gray-700 rounded-full flex items-center justify-center">-</button>
<span className="font-semibold">1</span>
<button className="w-8 h-8 bg-neon-purple text-white rounded-full flex items-center justify-center">+</button>
</div>
<span className="font-bold text-neon-purple">$25.00</span>
</div>
</div>
</div>
{/* Cart Summary */}
<div className="p-6 border-t border-gray-800 space-y-4">
<div className="space-y-2">
<div className="flex justify-between text-sm">
<span>Subtotal</span>
<span>$33.50</span>
</div>
<div className="flex justify-between text-sm">
<span>Tax</span>
<span>$3.35</span>
</div>
<div className="flex justify-between font-bold text-lg border-t border-gray-700 pt-2">
<span>Total</span>
<span className="text-neon-cyan">$36.85</span>
</div>
</div>
<button className="w-full bg-gradient-to-r from-neon-cyan to-neon-purple text-black font-bold py-4 rounded-xl hover:shadow-neon-purple transition-all duration-300 pulse-neon">
<FontAwesomeIcon icon={faCreditCard} className="mr-2" />
                    CHECKOUT
                </button>
</div>
</div>
          </>
        );

        export default CartPanel;
